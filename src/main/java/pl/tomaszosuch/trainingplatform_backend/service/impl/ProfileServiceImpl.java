package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.dto.request.ChangePasswordRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.DeleteAccountRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.UpdateProfileRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.LastAdminException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.InvitationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.ProfileService;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final InvitationRepository invitationRepository;

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

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Nieprawidłowe hasło");
        }

        if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new LastAdminException();
        }

        int revoked = invitationRepository.revokePendingByInviter(userId, LocalDateTime.now());

        userRepository.delete(user);

        log.warn("Usunięto konto {} (rola {}), unieważniono {} oczekujących zaproszeń",
                user.getEmail(), user.getRole(), revoked);
    }

}
