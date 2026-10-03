package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserCoachFlagMigrationTest")
class UserCoachFlagMigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final String MIGRATION_PATH = "db/migration/V19__user_coach_flag.sql";

    private static final String INSERT_COOPERATION = """
            INSERT INTO cooperation (coach_id, athlete_id, status, created_at, expires_at)
            VALUES (?, ?, ?, now(), now() + interval '14 days')
            """;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("uzupełnienie daje flagę trenerom z PENDING i ACTIVE - nie historii i nie podopiecznym")
    void shouldBackfillOnlyCurrentCoaches() throws IOException {
        Long activeCoach = user("aktywny@example.com");
        Long pendingCoach = user("oczekujacy@example.com");
        Long pastCoach = user("byly@example.com");
        Long athlete = user("podopieczny@example.com");
        Long outsider = user("obcy@example.com");

        cooperation(activeCoach, athlete, "ACTIVE");
        cooperation(pendingCoach, athlete, "PENDING");
        cooperation(pastCoach, athlete, "ENDED");

        jdbcTemplate.update("UPDATE users SET coach = false");

        jdbcTemplate.execute(migrationSql());

        assertTrue(isCoach(activeCoach));
        assertTrue(isCoach(pendingCoach));
        assertFalse(isCoach(pastCoach));
        assertFalse(isCoach(athlete));
        assertFalse(isCoach(outsider));
    }

    private Long user(String email) {
        return em.persistAndFlush(User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build()).getId();
    }

    private void cooperation(Long coachId, Long athleteId, String status) {
        jdbcTemplate.update(INSERT_COOPERATION, coachId, athleteId, status);
    }

    private boolean isCoach(Long userId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT coach FROM users WHERE id = ?", Boolean.class, userId));
    }

    private static String migrationSql() throws IOException {
        return new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8);
    }
}