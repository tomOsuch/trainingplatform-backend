package pl.tomaszosuch.trainingplatform_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.tomaszosuch.trainingplatform_backend.config.PasswordResetProperties;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetConfirmRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PasswordResetCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.PasswordResetToken;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidPasswordResetTokenException;
import pl.tomaszosuch.trainingplatform_backend.repository.PasswordResetTokenRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.security.ClientInfo;
import pl.tomaszosuch.trainingplatform_backend.security.RateLimiter;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.service.impl.PasswordResetServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImplTest")
public class PasswordResetServiceImplTest {

    private static final String EMAIL = "jan.kowalski@example.com";
    private static final ClientInfo KLIENT = new ClientInfo("127.0.0.1", "JUnit");
    private static final String PLAIN_TOKEN = "jawny-token-resetu_ABC123";
    private static final String TOKEN_HASH =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String RESET_BASE_URL = "http://localhost:3000/reset-password";

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecureTokenGenerator tokenGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RateLimiter rateLimiter;

    @Captor
    private ArgumentCaptor<PasswordResetToken> tokenCaptor;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    private PasswordResetProperties properties;
    private PasswordResetServiceImpl passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        properties = new PasswordResetProperties();
        properties.setExpirationMinutes(60);
        properties.setResetBaseUrl(RESET_BASE_URL);

        passwordResetService = new PasswordResetServiceImpl(
                tokenRepository,
                userRepository,
                tokenGenerator,
                passwordEncoder,
                emailService,
                properties,
                refreshTokenService,
                rateLimiter);

        user = User.builder()
                .id(4L)
                .email(EMAIL)
                .password("stary-hash")
                .firstName("Jan")
                .lastName("Kowalski")
                .role(Role.USER)
                .isActive(true)
                .build();
    }

    private void stubExistingAccount() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenGenerator.generateToken()).thenReturn(PLAIN_TOKEN);
        when(tokenGenerator.hash(PLAIN_TOKEN)).thenReturn(TOKEN_HASH);
    }

    private PasswordResetToken existingToken() {
        return PasswordResetToken.builder()
                .id(9L)
                .tokenHash(TOKEN_HASH)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    @Nested
    @DisplayName("Żdanie resetu")
    class Requesting {

        @Test
        @DisplayName("zapisuje skrót tokenu, nigdy token nie jest jawny")
        public void shouldStoreOnlyTokenHash() {
            stubExistingAccount();

            passwordResetService.requestReset(new PasswordResetRequest(EMAIL), KLIENT);

            verify(tokenRepository).save(tokenCaptor.capture());
            PasswordResetToken saved = tokenCaptor.getValue();

            assertEquals(TOKEN_HASH, saved.getTokenHash());
            assertNotEquals(PLAIN_TOKEN, saved.getTokenHash());
            assertEquals(user, saved.getUser());
        }

        @Test
        @DisplayName("kasuje poprzednie tokeny PRZED zapisem nowego")
        void shouldDeletePreviousTokensBeforeSaving() {
            stubExistingAccount();

            passwordResetService.requestReset(new PasswordResetRequest(EMAIL), KLIENT);

            InOrder order = inOrder(tokenRepository);
            order.verify(tokenRepository).deleteByUserId(4L);
            order.verify(tokenRepository).flush();
            order.verify(tokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("ustawia termin ważności zgodnie z konfiguracją")
        void shouldSetExpiryFromConfiguration() {
            properties.setExpirationMinutes(5);
            stubExistingAccount();

            passwordResetService.requestReset(new PasswordResetRequest(EMAIL), KLIENT);

            verify(tokenRepository).save(tokenCaptor.capture());
            LocalDateTime expiresAt = tokenCaptor.getValue().getExpiresAt();

            assertTrue(expiresAt.isAfter(LocalDateTime.now().plusMinutes(4)));
            assertTrue(expiresAt.isBefore(LocalDateTime.now().plusMinutes(6)));
        }

        @Test
        @DisplayName("wysyła link zbudowany z adresu bazowego i tokenu jawnego")
        void shouldSendLinkNotBareToken() {
            stubExistingAccount();

            passwordResetService.requestReset(new PasswordResetRequest(EMAIL), KLIENT);

            verify(emailService).sendPasswordReset(
                    eq(EMAIL), urlCaptor.capture(), any(LocalDateTime.class));
            String url = urlCaptor.getValue();

            assertTrue(url.startsWith(RESET_BASE_URL));
            assertTrue(url.contains(PLAIN_TOKEN));
            assertNotEquals(PLAIN_TOKEN, url);
        }

        @Test
        @DisplayName("nie robi nic dla adresu bez konta")
        void shouldDoNothingForUnknownEmail() {
            when(userRepository.findByEmail("nieznany@example.com")).thenReturn(Optional.empty());

            passwordResetService.requestReset(new PasswordResetRequest("nieznany@example.com"), KLIENT);

            verify(tokenRepository, never()).save(any(PasswordResetToken.class));
            verify(tokenRepository, never()).deleteByUserId(anyLong());
            verify(emailService, never()).sendPasswordReset(anyString(), anyString(), any());
            verify(rateLimiter).checkPasswordResetRequest(KLIENT.ip(), "nieznany@example.com");
        }

        @Test
        @DisplayName("obcina białe znaki wokół adresu")
        void shouldTrimEmail() {
            stubExistingAccount();

            passwordResetService.requestReset(new PasswordResetRequest("  " + EMAIL + "  "), KLIENT);

            verify(tokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("zachowuje token, gdy wysyłka maila zawiedzie")
        void shouldKeepTokenWhenDeliveryFails() {
            stubExistingAccount();
            doThrow(new RuntimeException("Serwer SMTP nie odpowiada"))
                    .when(emailService).sendPasswordReset(anyString(), anyString(), any());

            passwordResetService.requestReset(new PasswordResetRequest(EMAIL), KLIENT);

            verify(tokenRepository).save(any(PasswordResetToken.class));
        }
    }

    @Nested
    @DisplayName("Sprawdzenie tokenu")
    class Checking {

        @Test
        @DisplayName("zwraca adres i termin ważności")
        void shouldReturnEmailAndExpiry() {
            PasswordResetToken token = existingToken();
            when(tokenGenerator.hash(PLAIN_TOKEN)).thenReturn(TOKEN_HASH);
            when(tokenRepository.findByTokenHashWithUser(TOKEN_HASH))
                    .thenReturn(Optional.of(token));

            PasswordResetCheckResponse response = passwordResetService.checkToken(PLAIN_TOKEN);

            assertEquals(EMAIL, response.email());
            assertEquals(token.getExpiresAt(), response.expiresAt());
        }

        @Test
        @DisplayName("odrzuca token wygasły")
        void shouldRejectExpiredToken() {
            PasswordResetToken token = existingToken();
            token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

            when(tokenGenerator.hash(PLAIN_TOKEN)).thenReturn(TOKEN_HASH);
            when(tokenRepository.findByTokenHashWithUser(TOKEN_HASH)).thenReturn(Optional.of(token));

            InvalidPasswordResetTokenException ex = assertThrows(
                    InvalidPasswordResetTokenException.class,
                    () -> passwordResetService.checkToken(PLAIN_TOKEN));

            assertTrue(ex.getMessage().contains("wygasł"));
        }
    }

    @Nested
    @DisplayName("ustawienie nowego hasła")
    class Confirming {

        @Test
        @DisplayName("zapisuje zakodowane haslo i oznacza token jako wykorzystany")
        void shouldEncodePasswordAndConsumeToken() {
            PasswordResetToken token = existingToken();

            when(tokenGenerator.hash(PLAIN_TOKEN)).thenReturn(TOKEN_HASH);
            when(tokenRepository.findByTokenHashWithUser(TOKEN_HASH))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode("NoweHaslo123")).thenReturn("$2a$10$nowy-hash");

            passwordResetService.confirmReset(
                    new PasswordResetConfirmRequest(PLAIN_TOKEN, "NoweHaslo123"));

            assertEquals("$2a$10$nowy-hash", user.getPassword());
            assertNotEquals("NoweHaslo123", user.getPassword());
            assertNotNull(token.getUsedAt());

            verify(userRepository).save(user);
            verify(tokenRepository).save(token);
        }

        @Test
        @DisplayName("odrzuca token, którego nie ma w bazie")
        void shouldRejectUnknownToken() {
            when(tokenGenerator.hash("zmyslony")).thenReturn("inny-hash");
            when(tokenRepository.findByTokenHashWithUser("inny-hash"))
                    .thenReturn(Optional.empty());

            assertThrows(InvalidPasswordResetTokenException.class,
                    () -> passwordResetService.confirmReset(
                            new PasswordResetConfirmRequest("zmyslony", "NoweHaslo123")));

            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("odrzuca token już wykorzystany")
        void shouldRejectUsedToken() {
            PasswordResetToken token = existingToken();
            token.setUsedAt(LocalDateTime.now().minusMinutes(5));

            when(tokenGenerator.hash(PLAIN_TOKEN)).thenReturn(TOKEN_HASH);
            when(tokenRepository.findByTokenHashWithUser(TOKEN_HASH))
                    .thenReturn(Optional.of(token));

            InvalidPasswordResetTokenException ex = assertThrows(
                    InvalidPasswordResetTokenException.class,
                    () -> passwordResetService.confirmReset(
                            new PasswordResetConfirmRequest(PLAIN_TOKEN, "NoweHaslo123")));

            assertTrue(ex.getMessage().contains("wykorzystany"));
            assertEquals("stary-hash", user.getPassword());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("odrzuca token wygasły")
        void shouldRejectExpiredToken() {
            PasswordResetToken token = existingToken();
            token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

            when(tokenGenerator.hash(PLAIN_TOKEN)).thenReturn(TOKEN_HASH);
            when(tokenRepository.findByTokenHashWithUser(TOKEN_HASH))
                    .thenReturn(Optional.of(token));

            assertThrows(InvalidPasswordResetTokenException.class,
                    () -> passwordResetService.confirmReset(
                            new PasswordResetConfirmRequest(PLAIN_TOKEN, "NoweHaslo123")));

            assertEquals("stary-hash", user.getPassword());
        }
    }
}
