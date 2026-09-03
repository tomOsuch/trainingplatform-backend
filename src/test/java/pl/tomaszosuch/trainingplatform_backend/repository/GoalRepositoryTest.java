package pl.tomaszosuch.trainingplatform_backend.repository;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
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
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

import java.time.LocalDate;

import static org.junit.Assert.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("GoalRepositoryTest")
public class GoalRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private GoalRepository goalRepository;

    private User user;
    private WorkoutCategory category;

    @BeforeEach
    void setUp() {
        user = testEntityManager.persist(User.builder()
                .email("goal-test@example.com")
                .password("hash")
                .firstName("Jan")
                .lastName("Testowy")
                .role(Role.USER)
                .isActive(true)
                .build());
        category = testEntityManager.persist(WorkoutCategory.builder()
                .name("Taniec testowy")
                .build());
    }

    private Goal persistGoal() {
        return testEntityManager.persist(Goal.builder()
                .user(user)
                .category(category)
                .title("100 godzin tańca")
                .metric(GoalMetric.MINUTES)
                .targetValue(6000)
                .startDate(LocalDate.of(2026, 1, 1))
                .build());
    }

    @Test
    @DisplayName("usunięcie użytkownika kasuje jego cele (ON DELETE CASCADE)")
    void shouldCascadeDeleteGoalsWhenUserDeleted() {
        // given
        Long goalId = persistGoal().getId();
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        testEntityManager.remove(testEntityManager.find(User.class, user.getId()));
        testEntityManager.flush();
        testEntityManager.clear();

        // then
        assertFalse(goalRepository.existsById(goalId));
    }

    @Test
    @DisplayName("nie da się usunąć kategorii używanej przez cel (RESTRICT)")
    void shouldRejectCategoryDeleteWhenUsedByGoal() {
        // given
        persistGoal();
        testEntityManager.flush();
        testEntityManager.clear();

        // when
        testEntityManager.remove(testEntityManager.find(WorkoutCategory.class, category.getId()));

        // then — naruszenie klucza obcego wychodzi dopiero przy flushu
        assertThrows(PersistenceException.class, testEntityManager::flush);
    }

    @Test
    @DisplayName("existsByCategoryId widzi kategorię użytą w celu")
    void shouldDetectCategoryUsage() {
        assertFalse(goalRepository.existsByCategoryId(category.getId()));

        persistGoal();
        testEntityManager.flush();

        assertTrue(goalRepository.existsByCategoryId(category.getId()));
    }
}
