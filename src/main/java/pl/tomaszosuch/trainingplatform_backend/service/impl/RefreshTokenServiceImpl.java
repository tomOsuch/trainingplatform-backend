package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.tomaszosuch.trainingplatform_backend.config.RefreshTokenProperties;
import pl.tomaszosuch.trainingplatform_backend.entity.RefreshToken;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidRefreshTokenException;
import pl.tomaszosuch.trainingplatform_backend.repository.RefreshTokenRepository;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.service.RefreshTokenService;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int USER_AGENT_MAX_LENGTH = 512;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final RefreshTokenProperties properties;
    private final RefreshTokenRevoker refreshTokenRevoker;

    @Override
    public IssuedToken issue(User user, String userAgent) {
        NewToken created = createToken(user, userAgent);
        log.debug("Wydano token odświeżający dla użytkownika {}", user.getEmail());
        return new IssuedToken(created.rawToken(), created.entity().getExpiresAt());
    }

    @Override
    public RotationResult rotate(String rawToken, String userAgent) {
        RefreshToken current = refreshTokenRepository
                .findByTokenHashWithUser(tokenGenerator.hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Token odświeżający jest nieprawidłowy"));

        if (current.wasRotated()) {
            detectReuse(current);
        }

        if (!current.isUsable()) {
            throw new InvalidRefreshTokenException("Token odświeżający wygasł lub został unieważniony");
        }

        User user = current.getUser();

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidRefreshTokenException("Konto jest nieaktywne");
        }

        NewToken created = createToken(user, userAgent);
        current.setRevokedAt(LocalDateTime.now());
        current.setReplacedBy(created.entity());

        return new RotationResult(user, new IssuedToken(created.rawToken(), created.entity().getExpiresAt()));
    }

    @Override
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashWithUser(tokenGenerator.hash(rawToken))
                .filter(RefreshToken::isUsable)
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    log.debug("Unieważniono token odświeżający użytkownika {}", token.getUser().getEmail());
                });
    }

    @Override
    public void revokeAllForUser(Long userId) {
        int revoked = refreshTokenRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());

        if (revoked > 0) {
            log.info("Unieważniono {} aktywnych sesji użytkownika o id {}", revoked, userId);
        }
    }

    private void detectReuse(RefreshToken current) {
        Long userId = current.getUser().getId();
        String email = current.getUser().getEmail();

        int revoked = refreshTokenRevoker.revokeAllActive(userId);

        log.warn("Wykryto ponowne użycie tokena odświeżającego użytkownika {} — unieważniono {} aktywnych sesji",
                email, revoked);

        throw new InvalidRefreshTokenException("Sesja została unieważniona ze względów bezpieczeństwa");
    }

    private NewToken createToken(User user, String userAgent) {
        String rawToken = tokenGenerator.generateToken();

        RefreshToken entity = RefreshToken.builder()
                .tokenHash(tokenGenerator.hash(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(properties.getExpirationDays()))
                .userAgent(truncate(userAgent))
                .build();

        return new NewToken(refreshTokenRepository.save(entity), rawToken);
    }

    private String truncate(String userAgent) {
        if (userAgent == null || userAgent.length() <= USER_AGENT_MAX_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, USER_AGENT_MAX_LENGTH);
    }

    private record NewToken(RefreshToken entity, String rawToken) {
    }
}