package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.tomaszosuch.trainingplatform_backend.config.RefreshTokenProperties;
import pl.tomaszosuch.trainingplatform_backend.entity.RefreshToken;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidRefreshTokenException;
import pl.tomaszosuch.trainingplatform_backend.repository.RefreshTokenRepository;
import pl.tomaszosuch.trainingplatform_backend.security.ClientInfo;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.service.impl.RefreshTokenRevoker;
import pl.tomaszosuch.trainingplatform_backend.service.impl.RefreshTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenServiceImplTest")
public class RefreshTokenServiceImplTest {

    private static final String STARY_TOKEN = "stary-surowy-token";
    private static final String STARY_HASH =
            "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String NOWY_TOKEN = "nowy-surowy-token";
    private static final String NOWY_HASH =
            "2222222222222222222222222222222222222222222222222222222222222222";
    private static final int WAZNOSC_DNI = 14;
    private static final Long ID_UZYTKOWNIKA = 7L;
    private static final ClientInfo KLIENT = new ClientInfo("127.0.0.1", "JUnit/1.0");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private SecureTokenGenerator tokenGenerator;

    @Mock
    private RefreshTokenRevoker refreshTokenRevoker;

    @Captor
    private ArgumentCaptor<RefreshToken> tokenCaptor;

    private RefreshTokenServiceImpl refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties properties = new RefreshTokenProperties();
        properties.setExpirationDays(WAZNOSC_DNI);

        refreshTokenService = new RefreshTokenServiceImpl(
                refreshTokenRepository,
                tokenGenerator,
                properties,
                refreshTokenRevoker);

        user = User.builder()
                .id(ID_UZYTKOWNIKA)
                .email("jan.kowalski@example.com")
                .password("hash")
                .firstName("Jan")
                .lastName("Kowalski")
                .role(Role.USER)
                .isActive(true)
                .build();
    }

    private RefreshToken aktywnyToken() {
        return RefreshToken.builder()
                .id(1L)
                .tokenHash(STARY_HASH)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private void stubOdnalezienieStarego(RefreshToken token) {
        when(tokenGenerator.hash(STARY_TOKEN)).thenReturn(STARY_HASH);
        when(refreshTokenRepository.findByTokenHashWithUser(STARY_HASH))
                .thenReturn(Optional.of(token));
    }

    private void stubBrakStarego() {
        when(tokenGenerator.hash(STARY_TOKEN)).thenReturn(STARY_HASH);
        when(refreshTokenRepository.findByTokenHashWithUser(STARY_HASH))
                .thenReturn(Optional.empty());
    }

    private void stubWydanieNowego() {
        when(tokenGenerator.generateToken()).thenReturn(NOWY_TOKEN);
        when(tokenGenerator.hash(NOWY_TOKEN)).thenReturn(NOWY_HASH);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(wywolanie -> wywolanie.getArgument(0));
    }

    @Test
    @DisplayName("issue: w bazie ląduje skrót, a wywołujący dostaje surowy token")
    void shouldStoreHashAndReturnRawToken() {
        stubWydanieNowego();

        RefreshTokenService.IssuedToken wydany = refreshTokenService.issue(user, KLIENT);

        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken zapisany = tokenCaptor.getValue();

        assertEquals(NOWY_HASH, zapisany.getTokenHash());
        assertEquals(NOWY_TOKEN, wydany.token());
        assertSame(user, zapisany.getUser());
        assertEquals("JUnit/1.0", zapisany.getUserAgent());
    }

    @Test
    @DisplayName("issue: ważność liczy się z konfiguracji")
    void shouldSetExpirationFromProperties() {
        stubWydanieNowego();

        LocalDateTime przed = LocalDateTime.now();
        RefreshTokenService.IssuedToken wydany = refreshTokenService.issue(user, KLIENT);

        assertTrue(wydany.expiresAt().isAfter(przed.plusDays(WAZNOSC_DNI - 1)));
        assertTrue(wydany.expiresAt().isBefore(przed.plusDays(WAZNOSC_DNI + 1)));
    }

    @Test
    @DisplayName("issue: za długi user-agent jest obcinany do 512 znaków")
    void shouldTruncateLongUserAgent() {
        stubWydanieNowego();

        refreshTokenService.issue(user, new ClientInfo("127.0.0.1", "x".repeat(600)));

        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertEquals(512, tokenCaptor.getValue().getUserAgent().length());
    }

    @Test
    @DisplayName("issue: brak informacji o kliencie nie przerywa wydania tokena")
    void shouldIssueTokenWithoutClientInfo() {
        stubWydanieNowego();

        refreshTokenService.issue(user, null);

        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertNull(tokenCaptor.getValue().getUserAgent());
    }

    @Test
    @DisplayName("rotate: stary token zostaje unieważniony i wskazuje na następcę")
    void shouldRotateUsableToken() {
        RefreshToken stary = aktywnyToken();
        stubOdnalezienieStarego(stary);
        stubWydanieNowego();

        RefreshTokenService.RotationResult wynik = refreshTokenService.rotate(STARY_TOKEN, KLIENT);

        assertSame(user, wynik.user());
        assertEquals(NOWY_TOKEN, wynik.refreshToken().token());
        assertNotNull(stary.getRevokedAt());
        assertEquals(NOWY_HASH, stary.getReplacedBy().getTokenHash());
        assertTrue(stary.wasRotated());
    }

    @Test
    @DisplayName("rotate: nieznany token odrzucony bez wydania nowego")
    void shouldRejectUnknownToken() {
        stubBrakStarego();

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(STARY_TOKEN, KLIENT));

        assertEquals("Token odświeżający jest nieprawidłowy", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotate: token po wylogowaniu odrzucony, ale bez kasowania sesji")
    void shouldRejectRevokedTokenWithoutRevokingSessions() {
        RefreshToken uniewazniony = aktywnyToken();
        uniewazniony.setRevokedAt(LocalDateTime.now().minusMinutes(5));
        stubOdnalezienieStarego(uniewazniony);

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(STARY_TOKEN, KLIENT));

        assertEquals("Token odświeżający wygasł lub został unieważniony", ex.getMessage());

        verify(refreshTokenRevoker, never()).revokeAllActive(anyLong());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotate: token wygasły odrzucony")
    void shouldRejectExpiredToken() {
        RefreshToken wygasly = aktywnyToken();
        wygasly.setExpiresAt(LocalDateTime.now().minusDays(1));
        stubOdnalezienieStarego(wygasly);

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(STARY_TOKEN, KLIENT));

        assertEquals("Token odświeżający wygasł lub został unieważniony", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotate: ponowne użycie zrotowanego tokena kasuje wszystkie sesje")
    void shouldDetectTokenReuse() {
        RefreshToken zrotowany = aktywnyToken();
        zrotowany.setRevokedAt(LocalDateTime.now().minusMinutes(1));
        zrotowany.setReplacedBy(aktywnyToken());
        stubOdnalezienieStarego(zrotowany);
        when(refreshTokenRevoker.revokeAllActive(ID_UZYTKOWNIKA)).thenReturn(3);

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(STARY_TOKEN, KLIENT));

        assertEquals("Sesja została unieważniona ze względów bezpieczeństwa", ex.getMessage());
        verify(refreshTokenRevoker).revokeAllActive(ID_UZYTKOWNIKA);
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotate: nie odnawia sesji konta wyłączonego przez administratora")
    void shouldRejectRotationForInactiveAccount() {
        user.setIsActive(false);
        stubOdnalezienieStarego(aktywnyToken());

        InvalidRefreshTokenException ex = assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(STARY_TOKEN, KLIENT));

        assertEquals("Konto jest nieaktywne", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("revoke: używalny token dostaje znacznik unieważnienia")
    void shouldRevokeUsableToken() {
        RefreshToken token = aktywnyToken();
        stubOdnalezienieStarego(token);

        refreshTokenService.revoke(STARY_TOKEN);

        assertNotNull(token.getRevokedAt());
    }

    @Test
    @DisplayName("revoke: nie nadpisuje znacznika tokena już unieważnionego")
    void shouldNotOverwriteRevocationTimestamp() {
        LocalDateTime pierwotny = LocalDateTime.now().minusHours(2);
        RefreshToken token = aktywnyToken();
        token.setRevokedAt(pierwotny);
        stubOdnalezienieStarego(token);

        refreshTokenService.revoke(STARY_TOKEN);

        assertEquals(pierwotny, token.getRevokedAt());
    }

    @Test
    @DisplayName("revoke: nieznany token nie przerywa wylogowania")
    void shouldIgnoreUnknownTokenOnRevoke() {
        stubBrakStarego();

        assertDoesNotThrow(() -> refreshTokenService.revoke(STARY_TOKEN));
    }

    @Test
    @DisplayName("revokeAllForUser: unieważnia sesje użytkownika jednym zapytaniem")
    void shouldRevokeAllSessionsForUser() {
        when(refreshTokenRepository.revokeAllActiveByUserId(eq(ID_UZYTKOWNIKA), any(LocalDateTime.class)))
                .thenReturn(2);

        refreshTokenService.revokeAllForUser(ID_UZYTKOWNIKA);

        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(ID_UZYTKOWNIKA), any(LocalDateTime.class));
    }
}