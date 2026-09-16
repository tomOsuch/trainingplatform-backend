package pl.tomaszosuch.trainingplatform_backend.security;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.LastAdminException;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class LastAdminGuard {

    private final UserRepository userRepository;

    public void requireNotLastActiveAdmin(User user) {
        if (user.getRole() != Role.ADMIN || !Boolean.TRUE.equals(user.getIsActive())) {
            return;
        }
        if (userRepository.countActiveByRole(Role.ADMIN) <= 1) {
            throw new LastAdminException();
        }
    }
}