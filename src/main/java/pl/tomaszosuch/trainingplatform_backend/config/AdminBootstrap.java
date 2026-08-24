package pl.tomaszosuch.trainingplatform_backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    @Override
    @Transactional
    public void run(String... args) {

        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        if (!StringUtils.hasText(properties.getEmail())
                || !StringUtils.hasText(properties.getPassword())) {
            log.warn("Brak konta administratora, a zmienne ADMIN_EMAIL/ADMIN_PASSWORD "
                    + "nie są ustawione — bootstrap pominięty.");
            return;
        }

        String email = properties.getEmail().trim();

        userRepository.findByEmail(email)
                .ifPresentOrElse(this::promote, () -> create(email));
    }

    private void promote(User user) {
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        log.warn("Bootstrap: konto {} PROMOWANE do roli ADMIN.", user.getEmail());
    }

    private void create(String email) {
        User admin = User.builder()
                .email(email)
                .password(passwordEncoder.encode(properties.getPassword()))
                .firstName(properties.getFirstName())
                .lastName(properties.getLastName())
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        userRepository.save(admin);
        log.warn("Bootstrap: UTWORZONO konto administratora {}.", email);
    }
}