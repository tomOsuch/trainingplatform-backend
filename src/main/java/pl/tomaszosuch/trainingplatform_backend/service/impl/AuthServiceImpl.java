package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.LoginResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Invitation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailAlreadyRegisteredException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidCredentialsException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidInvitationException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidRefreshTokenException;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.InvitationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.security.JwtTokenProvider;
import pl.tomaszosuch.trainingplatform_backend.service.AuthService;
import pl.tomaszosuch.trainingplatform_backend.service.RefreshTokenService;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final InvitationRepository invitationRepository;
    private final SecureTokenGenerator invitationTokenGenerator;
    private final RefreshTokenService refreshTokenService;

    @Override
    public UserResponse register(RegisterRequest request) {

        Invitation invitation = requireUsableInvitation(request.token());

        String email = invitation.getEmail();

        if (!email.equalsIgnoreCase(request.email().trim())) {
            throw new InvalidInvitationException(
                    "Zaproszenie zostało wystawione na inny adres e-mail");
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(invitation.getRole())
                .isActive(true)
                .build();

        User saved = userRepository.save(user);

        invitation.setUsedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        if (invitation.getRole() == Role.ADMIN) {
            log.warn("Konto {} zarejestrowane z rolą ADMIN na podstawie zaproszenia (id={})",
                    email, invitation.getId());
        }

        return userMapper.toResponse(saved);
    }

    @Override
    public LoginResult login(LoginRequest request, String userAgent) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return buildLoginResult(user, refreshTokenService.issue(user, userAgent));
    }

    @Override
    public LoginResult refresh(String rawRefreshToken, String userAgent) {

        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new InvalidRefreshTokenException("Brak tokena odświeżającego");
        }

        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken, userAgent);

        return buildLoginResult(rotation.user(), rotation.refreshToken());
    }

    @Override
    public void logout(String rawRefreshToken) {

        if (StringUtils.hasText(rawRefreshToken)) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private LoginResult buildLoginResult(User user, RefreshTokenService.IssuedToken refreshToken) {

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());

        LoginResponse response = new LoginResponse(
                accessToken,
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return new LoginResult(response, refreshToken);
    }

    @Override
    public InvitationCheckResponse checkInvitation(String token) {
        Invitation invitation = requireUsableInvitation(token);
        return new InvitationCheckResponse(invitation.getEmail(), invitation.getExpiresAt());
    }

    private Invitation requireUsableInvitation(String token) {

        Invitation invitation = invitationRepository
                .findByTokenHash(invitationTokenGenerator.hash(token))
                .orElseThrow(() -> new InvalidInvitationException(
                        "Zaproszenie nie istnieje lub link jest nieprawidłowy"));

        if (invitation.getUsedAt() != null) {
            throw new InvalidInvitationException("Zaproszenie zostało już wykorzystane");
        }

        if (invitation.getRevokedAt() != null) {
            throw new InvalidInvitationException("Zaproszenie zostało unieważnione");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidInvitationException("Zaproszenie wygasło");
        }

        return invitation;
    }

}
