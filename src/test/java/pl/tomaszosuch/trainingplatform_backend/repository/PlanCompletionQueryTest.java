package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

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

/**
 * Sprawdza samo zapytanie agregujące plany na prawdziwym PostgreSQL — przede wszystkim regułę
 * odsiewania planów jeszcze przed użytkownikiem, której mockami nie da się przetestować.
 * „Dziś" jest parametrem, więc test nie zależy od daty uruchomienia.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PlanCompletionQueryTest")
class PlanCompletionQueryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TrainingPlanRepository trainingPlanRepository;

    private User user;
    private User otherUser;
    private WorkoutCategory dance;

    @BeforeEach
    void setUp() {
        user = em.persist(user("plans@example.com"));
        otherUser = em.persist(user("other@example.com"));
        dance = em.persist(WorkoutCategory.builder().name("Taniec").color("#9B59B6").build());
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private void plan(User owner, LocalDate date, PlanStatus status) {
        em.persist(TrainingPlan.builder()
                .user(owner).category(dance).title("trening")
                .plannedDate(date).status(status)
                .build());
    }

    private Map<PlanStatus, Long> countsFor(User owner) {
        em.flush();
        return trainingPlanRepository.countByStatus(owner.getId(), FROM, TO, TODAY).stream()
                .collect(Collectors.toMap(PlanStatusCountView::getStatus, PlanStatusCountView::getCount));
    }

    @Test
    @DisplayName("plan PLANNED z minioną datą liczy się jako nierozstrzygnięty")
    void shouldCountOverduePlannedAsUnresolved() {
        plan(user, LocalDate.of(2026, 3, 10), PlanStatus.PLANNED);
        plan(user, LocalDate.of(2026, 3, 12), PlanStatus.PLANNED);

        assertEquals(2L, countsFor(user).get(PlanStatus.PLANNED));
    }

    @Test
    @DisplayName("plan PLANNED z datą dzisiejszą lub przyszłą nie wchodzi do wyniku")
    void shouldIgnoreTodayAndFuturePlanned() {
        plan(user, TODAY, PlanStatus.PLANNED);                        // dziś — jeszcze przed nim
        plan(user, LocalDate.of(2026, 3, 25), PlanStatus.PLANNED);    // przyszłość

        assertNull(countsFor(user).get(PlanStatus.PLANNED));
    }

    @Test
    @DisplayName("rozstrzygnięte statusy liczą się niezależnie od tego, czy data minęła")
    void shouldCountResolvedStatusesRegardlessOfDate() {
        plan(user, LocalDate.of(2026, 3, 5), PlanStatus.COMPLETED);
        plan(user, LocalDate.of(2026, 3, 25), PlanStatus.COMPLETED);  // przyszła data, ale odhaczony
        plan(user, LocalDate.of(2026, 3, 6), PlanStatus.SKIPPED);
        plan(user, LocalDate.of(2026, 3, 28), PlanStatus.CANCELLED);  // odwołany z wyprzedzeniem

        Map<PlanStatus, Long> counts = countsFor(user);

        assertEquals(2L, counts.get(PlanStatus.COMPLETED));
        assertEquals(1L, counts.get(PlanStatus.SKIPPED));
        assertEquals(1L, counts.get(PlanStatus.CANCELLED));
    }

    @Test
    @DisplayName("granice zakresu są domknięte, plany spoza okresu odpadają")
    void shouldIncludeBothBoundaryDays() {
        plan(user, LocalDate.of(2026, 2, 28), PlanStatus.COMPLETED);  // dzień przed
        plan(user, FROM, PlanStatus.COMPLETED);
        plan(user, TO, PlanStatus.COMPLETED);
        plan(user, LocalDate.of(2026, 4, 1), PlanStatus.COMPLETED);   // dzień po

        assertEquals(2L, countsFor(user).get(PlanStatus.COMPLETED));
    }

    @Test
    @DisplayName("plany innego użytkownika nie wchodzą do wyniku")
    void shouldIgnoreOtherUsersPlans() {
        plan(otherUser, LocalDate.of(2026, 3, 5), PlanStatus.COMPLETED);
        plan(user, LocalDate.of(2026, 3, 6), PlanStatus.SKIPPED);

        Map<PlanStatus, Long> counts = countsFor(user);

        assertEquals(1, counts.size());
        assertEquals(1L, counts.get(PlanStatus.SKIPPED));
    }

    @Test
    @DisplayName("okres bez planów zwraca pustą listę")
    void shouldReturnEmptyListForPeriodWithoutPlans() {
        plan(user, LocalDate.of(2026, 1, 15), PlanStatus.COMPLETED);
        em.flush();

        assertTrue(trainingPlanRepository.countByStatus(user.getId(), FROM, TO, TODAY).isEmpty());
    }

}