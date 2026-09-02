package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.security.ClientInfo;

import java.time.LocalDateTime;

public interface RefreshTokenService {

    IssuedToken issue(User user, ClientInfo clientInfo);

    RotationResult rotate(String rawToken, ClientInfo clientInfo);

    void revoke(String rawToken);

    void revokeAllForUser(Long userId);

    record IssuedToken(String token, LocalDateTime expiresAt) {}

    record RotationResult(User user, IssuedToken refreshToken) {}
}
