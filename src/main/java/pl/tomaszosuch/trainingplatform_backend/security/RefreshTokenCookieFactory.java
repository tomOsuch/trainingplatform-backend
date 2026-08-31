package pl.tomaszosuch.trainingplatform_backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import pl.tomaszosuch.trainingplatform_backend.config.RefreshTokenProperties;
import pl.tomaszosuch.trainingplatform_backend.service.RefreshTokenService;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    private final RefreshTokenProperties properties;

    public ResponseCookie create(RefreshTokenService.IssuedToken token) {
        long maxAgeSeconds = Math.max(0, Duration.between(LocalDateTime.now(), token.expiresAt()).getSeconds());
        return base(token.token()).maxAge(maxAgeSeconds).build();
    }

    public ResponseCookie expired() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .path(properties.getCookiePath())
                .sameSite(properties.getCookieSameSite());
    }
}
