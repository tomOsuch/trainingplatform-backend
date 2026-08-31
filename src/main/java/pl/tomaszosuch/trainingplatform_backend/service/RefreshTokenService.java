package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.entity.User;

import java.time.LocalDateTime;

public interface RefreshTokenService {

    IssuedToken issue(User user, String userAgent);

    RotationResult rotate(String rawToken, String userAgent);

    void revoke(String rawToken);

    void revokeAllForUser(Long userId);

    record IssuedToken(String token, LocalDateTime expiresAt) {}

    record RotationResult(User user, IssuedToken refreshToken) {}
}
