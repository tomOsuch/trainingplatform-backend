package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Invitation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailAlreadyRegisteredException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidCredentialsException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidInvitationException;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.InvitationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.security.ClientInfo;
import pl.tomaszosuch.trainingplatform_backend.security.RateLimiter;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.security.JwtTokenProvider;
import pl.tomaszosuch.trainingplatform_backend.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImplTest")
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private SecureTokenGenerator invitationTokenGenerator;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String TOKEN = "jawny-token-zaproszenia";
    private static final String TOKEN_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final ClientInfo KLIENT = new ClientInfo("127.0.0.1", "JUnit");

    private RegisterRequest validRequest;
    private User savedUser;
    private Invitation invitation;
    private User inviter;


    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
                "Jan",
                "Kowalski",
                "jan.kowalski@example.com",
                "password",
                TOKEN);

        savedUser = User.builder()
                .id(1L)
                .email(validRequest.email())
                .firstName(validRequest.firstName())
                .lastName(validRequest.lastName())
                .password("encodedPassword")
                .role(Role.USER)
                .isActive(true)
                .build();

        inviter = User.builder()
                .id(99L)
                .email("admin@example.com")
                .firstName("Administrator")
                .lastName("Systemu")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        invitation = Invitation.builder()
                .id(5L)
                .email("jan.kowalski@example.com")
                .tokenHash(TOKEN_HASH)
                .role(Role.USER)
                .invitedBy(inviter)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private void stubValidInvitation() {
            when(invitationTokenGenerator.hash(TOKEN)).thenReturn(TOKEN_HASH);
            when(invitationRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(invitation));
        }

        @Test
        @DisplayName("rejestruje użytkownika na podstawie ważnego zaproszenia")
        void shouldRegisterUserFromValidInvitation() {
            stubValidInvitation();
            when(userRepository.existsByEmail("jan.kowalski@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn("haslo_zahashowane");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(userMapper.toResponse(any(User.class))).thenReturn(
                    new UserResponse(1L, "jan.kowalski@example.com", "Jan", "Kowalski",
                            LocalDate.of(1990, 5, 14), Role.USER));

            UserResponse response = authService.register(validRequest);

            assertNotNull(response);
            assertEquals(1L, response.id());
            assertEquals("jan.kowalski@example.com", response.email());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("nadaje rolę z zaproszenia, nie sztywne USER")
        void shouldTakeRoleFromInvitation() {
            invitation.setRole(Role.ADMIN);
            stubValidInvitation();
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            authService.register(validRequest);

            verify(userRepository).save(argThat(user -> user.getRole() == Role.ADMIN));
        }

        @Test
        @DisplayName("zapisuje adres z zaproszenia, nie z ciała żądania")
        void shouldUseEmailFromInvitationNotFromRequest() {
            RegisterRequest differentCase = new RegisterRequest(
                    "Jan", "Kowalski", "JAN.KOWALSKI@Example.com", "password", TOKEN);

            stubValidInvitation();
            when(userRepository.existsByEmail("jan.kowalski@example.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            authService.register(differentCase);

            verify(userRepository).save(argThat(
                    user -> user.getEmail().equals("jan.kowalski@example.com")));
        }

        @Test
        @DisplayName("oznacza zaproszenie jako wykorzystane")
        void shouldMarkInvitationAsUsed() {
            stubValidInvitation();
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            authService.register(validRequest);

            assertNotNull(invitation.getUsedAt());
            verify(invitationRepository).save(invitation);
        }

        @Test
        @DisplayName("hashuje hasło przed zapisem")
        void shouldHashPasswordBeforeSaving() {
            stubValidInvitation();
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn("$2a$10$zahashowane");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            authService.register(validRequest);

            verify(passwordEncoder).encode("password");
            verify(userRepository).save(argThat(
                    user -> user.getPassword().equals("$2a$10$zahashowane")));
        }

        @Test
        @DisplayName("odrzuca token, którego nie ma w bazie")
        void shouldRejectUnknownToken() {
            when(invitationTokenGenerator.hash(TOKEN)).thenReturn(TOKEN_HASH);
            when(invitationRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

            assertThrows(InvalidInvitationException.class,
                    () -> authService.register(validRequest));

            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("odrzuca zaproszenie już wykorzystane")
        void shouldRejectUsedInvitation() {
            invitation.setUsedAt(LocalDateTime.now().minusHours(1));
            stubValidInvitation();

            InvalidInvitationException ex = assertThrows(InvalidInvitationException.class,
                    () -> authService.register(validRequest));

            assertTrue(ex.getMessage().contains("wykorzystane"));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("odrzuca zaproszenie unieważnione")
        void shouldRejectRevokedInvitation() {
            invitation.setRevokedAt(LocalDateTime.now().minusMinutes(10));
            stubValidInvitation();

            InvalidInvitationException ex = assertThrows(InvalidInvitationException.class,
                    () -> authService.register(validRequest));

            assertTrue(ex.getMessage().contains("unieważnione"));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("odrzuca zaproszenie wygasłe")
        void shouldRejectExpiredInvitation() {
            invitation.setExpiresAt(LocalDateTime.now().minusDays(1));
            stubValidInvitation();

            InvalidInvitationException ex = assertThrows(InvalidInvitationException.class,
                    () -> authService.register(validRequest));

            assertTrue(ex.getMessage().contains("wygasło"));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("odrzuca rejestrację na adres inny niż z zaproszenia")
        void shouldRejectEmailMismatch() {
            RegisterRequest otherEmail = new RegisterRequest(
                    "Jan", "Kowalski", "ktos.inny@example.com", "password", TOKEN);

            stubValidInvitation();

            InvalidInvitationException ex = assertThrows(InvalidInvitationException.class,
                    () -> authService.register(otherEmail));

            assertTrue(ex.getMessage().contains("inny adres"));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("zwraca 409 przez EmailAlreadyRegisteredException, gdy adres ma już konto")
        void shouldThrowConflictWhenEmailAlreadyExists() {
            stubValidInvitation();
            when(userRepository.existsByEmail("jan.kowalski@example.com")).thenReturn(true);

            EmailAlreadyRegisteredException ex = assertThrows(
                    EmailAlreadyRegisteredException.class,
                    () -> authService.register(validRequest));

            assertTrue(ex.getMessage().contains("jan.kowalski@example.com"));
            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("nie oznacza zaproszenia jako wykorzystanego, gdy rejestracja się nie powiedzie")
        void shouldNotConsumeInvitationOnFailure() {
            stubValidInvitation();
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertThrows(EmailAlreadyRegisteredException.class,
                    () -> authService.register(validRequest));

            assertNull(invitation.getUsedAt());
            verify(invitationRepository, never()).save(any(Invitation.class));
        }
    }

    @Nested
    @DisplayName("checkInvitation()")
    class CheckInvitationTests {

        @Test
        @DisplayName("zwraca adres i termin ważności dla ważnego zaproszenia")
        void shouldReturnEmailAndExpiry() {
            when(invitationTokenGenerator.hash(TOKEN)).thenReturn(TOKEN_HASH);
            when(invitationRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(invitation));

            InvitationCheckResponse result = authService.checkInvitation(TOKEN);

            assertEquals("jan.kowalski@example.com", result.email());
            assertEquals(invitation.getExpiresAt(), result.expiresAt());
        }

        @Test
        @DisplayName("stosuje te same reguły co rejestracja — odrzuca wygasłe")
        void shouldApplySameRulesAsRegistration() {
            invitation.setExpiresAt(LocalDateTime.now().minusDays(1));
            when(invitationTokenGenerator.hash(TOKEN)).thenReturn(TOKEN_HASH);
            when(invitationRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(invitation));

            assertThrows(InvalidInvitationException.class,
                    () -> authService.checkInvitation(TOKEN));
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTestes {
        @Test
        @DisplayName("powinien zwrócić token gdy dane logowania są poprawne")
        public void shouldReturnTokenWhenCredentialsAreValid() {
            // given
            var loginRequest = new LoginRequest("jan.kowalski@example.com", "password");

            when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches(loginRequest.password(), savedUser.getPassword())).thenReturn(true);
            when(jwtTokenProvider.generateToken(savedUser.getEmail())).thenReturn("wygenerowany.jwt.token");
            when(refreshTokenService.issue(savedUser, KLIENT)).thenReturn(
                    new RefreshTokenService.IssuedToken("surowy-refresh-token", LocalDateTime.now().plusDays(14)));

            // when
            var result = authService.login(loginRequest, KLIENT);

            // then
            assertNotNull(result);
            assertEquals("wygenerowany.jwt.token", result.response().token());
            assertEquals("Bearer", result.response().type());
            assertEquals(savedUser.getId(), result.response().userId());
            assertEquals(savedUser.getEmail(), result.response().email());
            assertEquals(savedUser.getRole(), result.response().role());
            assertEquals("surowy-refresh-token", result.refreshToken().token());
        }

        @Test
        @DisplayName("powinien rzucić wyjątek gdy dane logowania są niepoprawne")
        public void shouldThrowExceptionWhenCredentialsAreInvalid() {
            // given
            var loginRequest = new pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest(
                    "nieistniejacy@example.com", "password");

            when(userRepository.findByEmail(loginRequest.email()))
                    .thenReturn(java.util.Optional.empty());

            // when & then
            InvalidCredentialsException ex = assertThrows(
                    InvalidCredentialsException.class,
                    () -> authService.login(loginRequest, KLIENT));

            assertTrue(ex.getMessage().contains("Nieprawidłowy"));
            verify(jwtTokenProvider, never()).generateToken(anyString());
            verify(rateLimiter).registerFailedLogin(loginRequest.email());
            verify(refreshTokenService, never()).issue(any(), any());
        }

    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy hasło jest błędne")
    public void shouldThrowExceptionWhenPasswordIsWrong() {
        // given
        var loginRequest = new pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest(
                "jan.kowalski@example.com", "zleHaslo");

        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches(loginRequest.password(), savedUser.getPassword())).thenReturn(false);

        // when & then
        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest, KLIENT));

        assertTrue(ex.getMessage().contains("Nieprawidłowy"));
        verify(jwtTokenProvider, never()).generateToken(anyString());
        verify(rateLimiter).registerFailedLogin(loginRequest.email());
        verify(refreshTokenService, never()).issue(any(), any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy konto jest nieaktywne")
    public void shouldThrowExceptionWhenAccountIsInactive() {
        // given
        var loginRequest = new pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest(
                "jan.kowalski@example.com", "password");

        User inactiveUser = User.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .password(savedUser.getPassword())
                .role(savedUser.getRole())
                .isActive(false)
                .build();

        when(userRepository.findByEmail(loginRequest.email()))
                .thenReturn(java.util.Optional.of(inactiveUser));

        // when & then
        InvalidCredentialsException ex = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest, KLIENT));

        assertTrue(ex.getMessage().contains("Nieprawidłowy"));
        verify(jwtTokenProvider, never()).generateToken(anyString());
        verify(rateLimiter).registerFailedLogin(loginRequest.email());
        verify(refreshTokenService, never()).issue(any(), any());
    }

    @Test
    @DisplayName("sprawdza limit ZANIM sięgnie do bazy")
    void shouldCheckRateLimitBeforeTouchingRepository() {
        var loginRequest = new LoginRequest("jan.kowalski@example.com", "password");

        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest, KLIENT));

        InOrder kolejnosc = inOrder(rateLimiter, userRepository);
        kolejnosc.verify(rateLimiter).checkLoginAttempt(KLIENT.ip(), loginRequest.email());
        kolejnosc.verify(userRepository).findByEmail(loginRequest.email());
    }

}
