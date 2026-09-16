package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutTemplate;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("WorkoutTemplateRepositoryTest")
class WorkoutTemplateRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkoutTemplateRepository workoutTemplateRepository;

    private User owner;
    private WorkoutCategory dance;

    @BeforeEach
    void setUp() {
        owner = em.persist(user("wlasciciel@example.com"));
        dance = em.persist(WorkoutCategory.builder()
                .name("Taniec testowy").color("#9B59B6").iconName("music").build());
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private WorkoutTemplate template(User user, String name, Integer durationMin) {
        return em.persist(WorkoutTemplate.builder()
                .user(user).category(dance).name(name).durationMin(durationMin)
                .build());
    }

    @Test
    @DisplayName("lista szablonów obejmuje tylko właściciela, posortowana po nazwie")
    void shouldListOwnTemplatesSortedByName() {
        User other = em.persist(user("obcy@example.com"));
        template(owner, "Wtorkowy trening", 60);
        template(owner, "Poranna rozgrzewka", 20);
        template(other, "Cudzy szablon", 45);
        em.flush();

        List<WorkoutTemplate> templates = workoutTemplateRepository.findByUserIdOrderByNameAsc(owner.getId());

        assertEquals(List.of("Poranna rozgrzewka", "Wtorkowy trening"),
                templates.stream().map(WorkoutTemplate::getName).toList());
    }

    @Test
    @DisplayName("usunięcie konta kasuje jego szablony")
    void shouldCascadeDeleteTemplatesWhenUserDeleted() {
        template(owner, "Wtorkowy trening", 60);
        em.flush();
        em.clear();

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", owner.getId());

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workout_template WHERE user_id = ?", Integer.class, owner.getId()));
    }

    @Test
    @DisplayName("baza odrzuca niedodatni czas trwania")
    void shouldRejectNonPositiveDuration() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO workout_template (user_id, category_id, name, duration_min, created_at)
                        VALUES (?, ?, 'Zero minut', 0, now())
                        """, owner.getId(), dance.getId()));
    }

    @Test
    @DisplayName("czas trwania może być pusty")
    void shouldAllowNullDuration() {
        WorkoutTemplate saved = template(owner, "Bez czasu", null);
        em.flush();

        assertEquals(1, workoutTemplateRepository.findByUserIdOrderByNameAsc(owner.getId()).size());
        assertEquals(saved.getId(), workoutTemplateRepository.findByUserIdOrderByNameAsc(owner.getId()).get(0).getId());
    }

    @Test
    @DisplayName("szablon blokuje usunięcie kategorii na poziomie klucza obcego")
    void shouldBlockCategoryDeletionAtForeignKey() {
        template(owner, "Wtorkowy trening", 60);
        em.flush();

        assertTrue(workoutTemplateRepository.existsByCategoryId(dance.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("DELETE FROM workout_category WHERE id = ?", dance.getId()));
    }
}