package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.dto.request.ChangePasswordRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.DeleteAccountRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.NotificationPreferencesRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.UpdateProfileRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.NotificationPreferencesResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationConflictException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.CooperationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.InvitationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.security.LastAdminGuard;
import pl.tomaszosuch.trainingplatform_backend.service.ProfileService;
import pl.tomaszosuch.trainingplatform_backend.service.RefreshTokenService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final String ACTIVE_ATHLETES_MESSAGE =
            "Masz aktywnych podopiecznych (%d) — zakończ współpracę, zanim wyłączysz tryb trenera";

    private final CooperationRepository cooperationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final InvitationRepository invitationRepository;
    private final RefreshTokenService refreshTokenService;
    private final LastAdminGuard lastAdminGuard;

    @Override
    public UserResponse getProfile(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBirthDate(request.birthDate());


        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void changePassword(Long id, ChangePasswordRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        String newPassword = request.newPassword();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Podane hasło jest nieprawidłowe");
        }

        if (!newPassword.equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Hasła nie są zgodne");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Nowe hasło musi różnić się od aktualnego");
        }

        String email = user.getEmail();

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(id);

        log.info("Zmieniono hasło konta {} — unieważniono wszystkie sesje", email);
    }

    @Override
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Nieprawidłowe hasło");
        }

        lastAdminGuard.requireNotLastActiveAdmin(user);

        int revoked = invitationRepository.revokePendingByInviter(userId, LocalDateTime.now());

        userRepository.delete(user);

        log.warn("Usunięto konto {} (rola {}), unieważniono {} oczekujących zaproszeń",
                user.getEmail(), user.getRole(), revoked);
    }

    @Override
    public NotificationPreferencesResponse getNotificationPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return userMapper.toNotificationPreferences(user);
    }

    @Override
    public NotificationPreferencesResponse updateNotificationPreferences(Long userId, NotificationPreferencesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setRemindersEnabled(request.remindersEnabled());
        user.setReminderHoursBefore(request.reminderHoursBefore());

        log.info("Zmieniono preferencje przypomnień konta {}: włączone={}, wyprzedzenie={} h",
                user.getEmail(), request.remindersEnabled(), request.reminderHoursBefore());

        return userMapper.toNotificationPreferences(userRepository.save(user));
    }

    @Override
    public UserResponse setCoachMode(Long userId, boolean enabled) {
        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.isCoach() == enabled) {
            return userMapper.toResponse(user);
        }

        if (!enabled) {
            List<Cooperation> open = cooperationRepository.lockOpenByCoachId(userId);

            long active = open.stream()
                    .filter(cooperation -> cooperation.getStatus() == CooperationStatus.ACTIVE)
                    .count();

            if (active > 0) {
                throw new CooperationConflictException(ACTIVE_ATHLETES_MESSAGE.formatted(active));
            }

            LocalDateTime now = LocalDateTime.now();
            open.forEach(invitation -> {
                invitation.setStatus(CooperationStatus.WITHDRAWN);
                invitation.setEndedAt(now);
            });

            log.info("Konto {} wyłącza tryb trenera — wycofano {} oczekujących zaproszeń",
                    user.getEmail(), open.size());
        }

        user.setCoach(enabled);

        return userMapper.toResponse(userRepository.save(user));
    }

}
