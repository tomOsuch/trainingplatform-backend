package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("TrainingPlanAuthorConstraintTest")
class TrainingPlanAuthorConstraintTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final String INSERT = """
            INSERT INTO training_plan (user_id, category_id, created_by_user_id,
                                       title, planned_date, status, created_at)
            VALUES (?, ?, ?, ?, current_date, 'PLANNED', now())
            """;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User athlete;
    private User coach;
    private WorkoutCategory category;

    @BeforeEach
    void setUp() {
        athlete = em.persist(user("podopieczny@example.com"));
        coach = em.persist(user("trener@example.com"));
        category = em.persist(WorkoutCategory.builder()
                .name("Taniec testowy").color("#9B59B6").build());
        em.flush();
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    @Test
    @DisplayName("autor nie może być właścicielem planu - własny plan ma NULL, nie siebie")
    void shouldRejectSelfAuthorship() {
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT,
                        athlete.getId(), category.getId(), athlete.getId(), "Sam sobie"));

        assertTrue(ex.getMessage().contains("training_plan_author_not_owner_check"));
    }

    @Test
    @DisplayName("plan własny przechodzi z pustym autorem")
    void shouldAcceptPlanWithoutAuthor() {
        jdbcTemplate.update(INSERT,
                athlete.getId(), category.getId(), null, "Mój plan");

        assertNull(authorOf("Mój plan"));
    }

    @Test
    @DisplayName("plan od trenera zapisuje jego identyfikator")
    void shouldAcceptPlanAuthoredByAnotherUser() {
        jdbcTemplate.update(INSERT,
                athlete.getId(), category.getId(), coach.getId(), "Interwały");

        assertEquals(coach.getId(), authorOf("Interwały"));
    }

    @Test
    @DisplayName("usunięcie konta trenera zostawia plan podopiecznego, gubi tylko autora")
    void shouldKeepPlanWhenAuthorAccountIsDeleted() {
        jdbcTemplate.update(INSERT,
                athlete.getId(), category.getId(), coach.getId(), "Interwały");

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", coach.getId());

        assertEquals(1, (int) jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM training_plan WHERE title = 'Interwały'", Integer.class));
        assertNull(authorOf("Interwały"));
    }

    private Long authorOf(String title) {
        return jdbcTemplate.queryForObject(
                "SELECT created_by_user_id FROM training_plan WHERE title = ?", Long.class, title);
    }
}