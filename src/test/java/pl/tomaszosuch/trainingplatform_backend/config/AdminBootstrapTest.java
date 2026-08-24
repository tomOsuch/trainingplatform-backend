package pl.tomaszosuch.trainingplatform_backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBootstrapTest")
public class AdminBootstrapTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "Tajne123!";
    private static final String ENCODED_PASSWORD = "$2a$10$zakodowane";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private AdminBootstrapProperties properties;
    private AdminBootstrap adminBootstrap;

    @BeforeEach
    void setUp() {
        properties = new AdminBootstrapProperties();
        properties.setEmail(ADMIN_EMAIL);
        properties.setPassword(ADMIN_PASSWORD);

        adminBootstrap = new AdminBootstrap(userRepository, passwordEncoder, properties);
    }

    @Nested
    @DisplayName("Gdy administrator już istnieje")
    class AdminAlreadyExists {

        @Test
        @DisplayName("nie zapisuje niczego do bazy")
        void shouldDoNothing() {
            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

            adminBootstrap.run();

            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Gdy brak zmiennych konfiguracyjnych")
    class MissingConfiguration {

        @Test
        @DisplayName("pomija seed, gdy adres nie jest ustawiony")
        void shouldSkipWhenEmailMissing() {
            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
            properties.setEmail(null);

            adminBootstrap.run();

            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("pomija seed, gdy hasło jest puste")
        void shouldSkipWhenPasswordBlank() {
            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
            properties.setPassword("   ");

            adminBootstrap.run();

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Gdy konto o podanym adresie już istnieje")
    class AccountExists {

        @Test
        @DisplayName("promuje je do roli ADMIN i nie rusza hasła")
        void shouldPromoteExistingAccount() {
            User existing = User.builder()
                    .id(7L)
                    .email(ADMIN_EMAIL)
                    .password("stary-hash")
                    .firstName("Tomasz")
                    .lastName("Osuch")
                    .role(Role.USER)
                    .isActive(true)
                    .build();

            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existing));

            adminBootstrap.run();

            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();

            assertEquals(Role.ADMIN, saved.getRole());
            assertEquals("stary-hash", saved.getPassword());
            assertEquals("Tomasz", saved.getFirstName());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    @Nested
    @DisplayName("Gdy konta nie ma")
    class AccountMissing {

        @Test
        @DisplayName("zakłada aktywne konto z rolą ADMIN i zakodowanym hasłem")
        void shouldCreateAdminAccount() {
            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            adminBootstrap.run();

            verify(userRepository).save(userCaptor.capture());
            User created = userCaptor.getValue();

            assertEquals(ADMIN_EMAIL, created.getEmail());
            assertEquals(Role.ADMIN, created.getRole());
            assertTrue(created.getIsActive());
            assertEquals("Administrator", created.getFirstName());
            assertEquals("Systemu", created.getLastName());
            assertEquals(ENCODED_PASSWORD, created.getPassword());
            assertNotEquals(ADMIN_PASSWORD, created.getPassword());
        }

        @Test
        @DisplayName("obcina białe znaki wokół adresu")
        void shouldTrimEmail() {
            properties.setEmail("  " + ADMIN_EMAIL + "  ");

            when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
            when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            adminBootstrap.run();

            verify(userRepository).save(userCaptor.capture());
            assertEquals(ADMIN_EMAIL, userCaptor.getValue().getEmail());
        }
    }
}