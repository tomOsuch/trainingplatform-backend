package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import pl.tomaszosuch.trainingplatform_backend.config.PasswordResetProperties;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetConfirmRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PasswordResetCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.PasswordResetToken;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidPasswordResetTokenException;
import pl.tomaszosuch.trainingplatform_backend.repository.PasswordResetTokenRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.security.ClientInfo;
import pl.tomaszosuch.trainingplatform_backend.security.RateLimiter;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.service.EmailService;
import pl.tomaszosuch.trainingplatform_backend.service.PasswordResetService;
import pl.tomaszosuch.trainingplatform_backend.service.RefreshTokenService;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetProperties properties;
    private final RefreshTokenService refreshTokenService;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional(readOnly = true)
    public PasswordResetCheckResponse checkToken(String token) {

        PasswordResetToken resetToken = requireUsableToken(token);

        return new PasswordResetCheckResponse(resetToken.getUser().getEmail(), resetToken.getExpiresAt());
    }

    @Override
    public void confirmReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = requireUsableToken(request.token());

        User user = resetToken.getUser();
        String email = user.getEmail();
        Long userId = user.getId();

        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        refreshTokenService.revokeAllForUser(userId);

        log.info("Hasło konta {} zostało zresetowane — unieważniono wszystkie sesje", email);
    }

    @Override
    public void requestReset(PasswordResetRequest request, ClientInfo clientInfo) {
        String email = request.email().trim();

        rateLimiter.checkPasswordResetRequest(clientInfo.ip(), email);

        userRepository.findByEmail(email).ifPresentOrElse(
                this::issueToken,
                () -> log.info("Żądanie resetu hasła dla adresu bez konta: {}", email)
        );
    }

    private void issueToken(User user) {
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        String token = tokenGenerator.generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(properties.getExpirationMinutes());

        tokenRepository.save(PasswordResetToken.builder()
                .tokenHash(tokenGenerator.hash(token))
                .user(user)
                .expiresAt(expiresAt)
                .build());

        try {
            emailService.sendPasswordReset(user.getEmail(), buildResetUrl(token), expiresAt);
        } catch (RuntimeException ex) {
            log.error("Nie udało się wysłać maila z resetem hasła na adres {}: {}",
                    user.getEmail(), ex.getMessage(), ex);
        }
    }

    private PasswordResetToken requireUsableToken(String token) {

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHashWithUser(tokenGenerator.hash(token))
                .orElseThrow(() -> new InvalidPasswordResetTokenException(
                        "Link do resetu hasła jest nieprawidłowy"));

        if (resetToken.getUsedAt() != null) {
            throw new InvalidPasswordResetTokenException(
                    "Link do resetu hasła został już wykorzystany");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidPasswordResetTokenException("Link do resetu hasła wygasł");
        }

        return resetToken;
    }

    private String buildResetUrl(String token) {
        return UriComponentsBuilder.fromUriString(properties.getResetBaseUrl())
                .queryParam("token", token)
                .toUriString();
    }
}
