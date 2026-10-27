package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.support.PostgresContainerSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("WorkoutLogDetachQueryTest")
class WorkoutLogDetachQueryTest extends PostgresContainerSupport {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WorkoutLogRepository workoutLogRepository;

    private User owner;
    private User otherUser;
    private WorkoutCategory running;

    @BeforeEach
    void setUp() {
        owner = em.persist(user("wlasciciel@example.com"));
        otherUser = em.persist(user("obcy@example.com"));
        running = em.persist(WorkoutCategory.builder()
                .name("Bieganie testowe").color("#3498DB").iconName("activity").build());
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private TrainingPlan plan(User owner) {
        return em.persist(TrainingPlan.builder()
                .user(owner).category(running).title("Interwały").plannedDate(DAY)
                .build());
    }

    private WorkoutLog log(User owner, TrainingPlan plan) {
        return em.persist(WorkoutLog.builder()
                .user(owner).category(running).plan(plan).performedDate(DAY).durationMin(45)
                .build());
    }

    private long countLogs() {
        return em.getEntityManager()
                .createQuery("SELECT COUNT(l) FROM WorkoutLog l", Long.class)
                .getSingleResult();
    }

    @Test
    @DisplayName("odpina wpisy wskazanego planu i tylko je")
    void shouldDetachOnlyLogsOfGivenPlan() {
        TrainingPlan target = plan(owner);
        TrainingPlan otherPlanOfOwner = plan(owner);
        TrainingPlan planOfOtherUser = plan(otherUser);

        Long fromTarget1 = log(owner, target).getId();
        Long fromTarget2 = log(owner, target).getId();
        Long fromOtherPlan = log(owner, otherPlanOfOwner).getId();
        Long adHoc = log(owner, null).getId();
        Long foreign = log(otherUser, planOfOtherUser).getId();
        em.flush();

        int detached = workoutLogRepository.detachLogsFromPlan(target.getId());

        em.clear();

        assertEquals(2, detached);
        assertNull(em.find(WorkoutLog.class, fromTarget1).getPlan());
        assertNull(em.find(WorkoutLog.class, fromTarget2).getPlan());
        assertEquals(otherPlanOfOwner.getId(), em.find(WorkoutLog.class, fromOtherPlan).getPlan().getId());
        assertNull(em.find(WorkoutLog.class, adHoc).getPlan());
        assertEquals(planOfOtherUser.getId(), em.find(WorkoutLog.class, foreign).getPlan().getId());
        assertEquals(5, countLogs());
    }

    @Test
    @DisplayName("plan bez wpisów: zero zmian")
    void shouldDetachNothingWhenPlanHasNoLogs() {
        TrainingPlan empty = plan(owner);
        Long unrelated = log(owner, plan(owner)).getId();
        em.flush();

        assertEquals(0, workoutLogRepository.detachLogsFromPlan(empty.getId()));

        em.clear();
        assertEquals(1, countLogs());
        assertEquals(false, em.find(WorkoutLog.class, unrelated).getPlan() == null);
    }

    @Test
    @DisplayName("po odpięciu plan da się usunąć, a wpisy zostają w dzienniku")
    void shouldAllowDeletingPlanAfterDetach() {
        TrainingPlan target = plan(owner);
        Long logId = log(owner, target).getId();
        em.flush();

        workoutLogRepository.detachLogsFromPlan(target.getId());
        em.clear();

        em.remove(em.find(TrainingPlan.class, target.getId()));
        em.flush();
        em.clear();

        assertNull(em.find(TrainingPlan.class, target.getId()));
        assertNull(em.find(WorkoutLog.class, logId).getPlan());
        assertEquals(1, countLogs());
    }

    @Test
    @DisplayName("kontrola: bez odpięcia usunięcie planu z wpisami narusza FK")
    void shouldFailToDeletePlanWithLogsWithoutDetach() {
        TrainingPlan target = plan(owner);
        log(owner, target);
        em.flush();
        em.clear();

        em.remove(em.find(TrainingPlan.class, target.getId()));

        assertThrows(PersistenceException.class, () -> em.flush());
    }

}