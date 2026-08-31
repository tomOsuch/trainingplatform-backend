package pl.tomaszosuch.trainingplatform_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tomaszosuch.trainingplatform_backend.config.RefreshTokenProperties;
import pl.tomaszosuch.trainingplatform_backend.entity.RefreshToken;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.repository.RefreshTokenRepository;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.service.impl.RefreshTokenRevoker;
import pl.tomaszosuch.trainingplatform_backend.service.impl.RefreshTokenServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private SecureTokenGenerator tokenGenerator;

    @Mock
    private RefreshTokenRevoker refreshTokenRevoker;

    @Captor
    private ArgumentCaptor<RefreshToken> tokenCaptor;

    private RefreshTokenProperties properties;
    private RefreshTokenServiceImpl refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        properties = new RefreshTokenProperties();
        properties.setExpirationDays(WAZNOSC_DNI);
        // Pozostałe pola opisują wyłącznie budowę ciasteczka i serwis ich nie czyta.

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

    private void stubWydanieNowego() {
        when(tokenGenerator.generateToken()).thenReturn(NOWY_TOKEN);
        when(tokenGenerator.hash(NOWY_TOKEN)).thenReturn(NOWY_HASH);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(wywolanie -> wywolanie.getArgument(0));
    }


}
