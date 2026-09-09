package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ReminderCandidatesQueryTest")
class ReminderCandidatesQueryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    private User enabled;
    private User disabled;
    private WorkoutCategory category;

    @BeforeEach
    void setUp() {
        enabled = em.persist(user("chce@example.com", true));
        disabled = em.persist(user("niechce@example.com", false));
        category = em.persist(WorkoutCategory.builder().name("Taniec").build());
    }

    private static User user(String email, boolean remindersEnabled) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .remindersEnabled(remindersEnabled).reminderHoursBefore(24)
                .build();
    }

    private TrainingPlan plan(User owner, LocalDate date, PlanStatus status, LocalDateTime reminderSentAt) {
        return em.persist(TrainingPlan.builder()
                .user(owner).category(category).title("Salsa")
                .plannedDate(date).status(status).reminderSentAt(reminderSentAt)
                .build());
    }

    private List<TrainingPlan> candidates() {
        em.flush();
        return trainingPlanRepository.findReminderCandidates(LocalDate.now());
    }

    @Test
    @DisplayName("bierze tylko plany PLANNED użytkownika z włączonymi przypomnieniami")
    void shouldFilterByStatusAndPreference() {
        TrainingPlan expected = plan(enabled, TOMORROW, PlanStatus.PLANNED, null);
        plan(enabled, TOMORROW, PlanStatus.COMPLETED, null);
        plan(enabled, TOMORROW, PlanStatus.CANCELLED, null);
        plan(enabled, TOMORROW, PlanStatus.SKIPPED, null);
        plan(disabled, TOMORROW, PlanStatus.PLANNED, null);

        List<TrainingPlan> result = candidates();

        assertEquals(1, result.size());
        assertEquals(expected.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("plan z odnotowanym przypomnieniem i plan z przeszłości odpadają")
    void shouldSkipAlreadyRemindedAndPastPlans() {
        plan(enabled, TOMORROW, PlanStatus.PLANNED, LocalDateTime.now().minusHours(1));
        plan(enabled, LocalDate.now().minusDays(1), PlanStatus.PLANNED, null);

        assertTrue(candidates().isEmpty());
    }

    @Test
    @DisplayName("zajęcie planu działa raz — drugie wywołanie nie zmienia nic")
    void shouldClaimPlanOnlyOnce() {
        TrainingPlan plan = plan(enabled, TOMORROW, PlanStatus.PLANNED, null);
        em.flush();
        em.clear();

        LocalDateTime now = LocalDateTime.now();
        assertEquals(1, trainingPlanRepository.markReminderSent(plan.getId(), now));
        assertEquals(0, trainingPlanRepository.markReminderSent(plan.getId(), now.plusHours(1)));

        assertTrue(candidates().isEmpty());
    }

}