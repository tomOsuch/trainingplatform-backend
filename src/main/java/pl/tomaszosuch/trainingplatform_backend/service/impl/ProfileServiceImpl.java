package pl.tomaszosuch.trainingplatform_backend.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.ChangePasswordRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.UpdateProfileRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.ProfileService;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

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

}
