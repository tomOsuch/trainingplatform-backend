package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("NotificationDefaultsTest")
class NotificationDefaultsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("nowe konto ma przypomnienia wyłączone i wyprzedzenie 24 h")
    void shouldDefaultToDisabledForNewAccount() {
        User saved = em.persist(User.builder()
                .email("nowy@example.com").password("hash")
                .firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build());
        em.flush();
        em.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();

        assertFalse(reloaded.getRemindersEnabled());
        assertEquals(24, reloaded.getReminderHoursBefore());
    }

    @Test
    @DisplayName("konto założone bez tych kolumn dostaje wartości domyślne z migracji")
    void shouldApplyDatabaseDefaultsForExistingRow() {
        // Wiersz wstawiony z pominięciem nowych kolumn odtwarza konto sprzed migracji V8.
        em.getEntityManager().createNativeQuery("""
                INSERT INTO users (email, password, first_name, last_name, role, is_active, created_at)
                VALUES ('stary@example.com', 'hash', 'Anna', 'Testowa', 'USER', true, now())
                """).executeUpdate();
        em.clear();

        User existing = userRepository.findByEmail("stary@example.com").orElseThrow();

        assertFalse(existing.getRemindersEnabled());
        assertEquals(24, existing.getReminderHoursBefore());
    }

}