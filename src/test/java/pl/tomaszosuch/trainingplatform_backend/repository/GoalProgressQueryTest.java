package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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

import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.repository.specification.WorkoutLogSpecifications;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("GoalProgressQueryTest")
class GoalProgressQueryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private WorkoutLogRepository workoutLogRepository;

    private User user;
    private User otherUser;
    private WorkoutCategory dance;
    private WorkoutCategory gym;

    @BeforeEach
    void setUp() {
        user = em.persist(user("progress@example.com"));
        otherUser = em.persist(user("other@example.com"));
        dance = em.persist(WorkoutCategory.builder().name("Taniec").build());
        gym = em.persist(WorkoutCategory.builder().name("Siłownia").build());
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private Goal goal(User owner, WorkoutCategory category, GoalMetric metric,
                      LocalDate start, LocalDate end) {
        return em.persist(Goal.builder()
                .user(owner).category(category).title("cel").metric(metric).targetValue(100)
                .startDate(start).endDate(end)
                .build());
    }

    private void log(User owner, WorkoutCategory category, LocalDate date, Integer minutes) {
        em.persist(WorkoutLog.builder()
                .user(owner).category(category).performedDate(date).durationMin(minutes)
                .build());
    }

    private Map<Long, GoalProgressView> progressOf(User owner) {
        em.flush();
        return goalRepository.findActiveProgressByUserId(owner.getId()).stream()
                .collect(Collectors.toMap(GoalProgressView::getGoalId, Function.identity()));
    }

    @Test
    @DisplayName("wpis spoza okna nie wlicza się; granice okna są domknięte")
    void shouldCountOnlyLogsInsideWindow() {
        Goal g = goal(user, null, GoalMetric.SESSIONS,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        log(user, dance, LocalDate.of(2026, 2, 28), 60);   // dzień przed
        log(user, dance, LocalDate.of(2026, 3, 1), 60);    // pierwszy dzień
        log(user, dance, LocalDate.of(2026, 3, 31), 60);   // ostatni dzień
        log(user, dance, LocalDate.of(2026, 4, 1), 60);    // dzień po

        GoalProgressView row = progressOf(user).get(g.getId());

        assertEquals(2L, row.getSessions());
        assertEquals(120L, row.getMinutes());
    }

    @Test
    @DisplayName("cel bez daty końcowej liczy wpisy bez górnej granicy")
    void shouldHaveNoUpperBoundWhenEndDateIsNull() {
        Goal g = goal(user, null, GoalMetric.SESSIONS, LocalDate.of(2026, 1, 1), null);
        log(user, dance, LocalDate.of(2025, 12, 31), 60);  // przed startem
        log(user, dance, LocalDate.of(2026, 1, 1), 60);
        log(user, dance, LocalDate.of(2030, 1, 1), 60);    // daleka przyszłość

        assertEquals(2L, progressOf(user).get(g.getId()).getSessions());
    }

    @Test
    @DisplayName("cel z kategorią liczy tylko jej wpisy, cel bez kategorii — wszystkie")
    void shouldFilterByCategoryOnlyWhenGoalHasOne() {
        Goal danceOnly = goal(user, dance, GoalMetric.MINUTES, LocalDate.of(2026, 1, 1), null);
        Goal anything = goal(user, null, GoalMetric.MINUTES, LocalDate.of(2026, 1, 1), null);
        log(user, dance, LocalDate.of(2026, 2, 1), 90);
        log(user, gym, LocalDate.of(2026, 2, 2), 45);

        Map<Long, GoalProgressView> rows = progressOf(user);

        assertEquals(90L, rows.get(danceOnly.getId()).getMinutes());
        assertEquals(135L, rows.get(anything.getId()).getMinutes());
    }

    @Test
    @DisplayName("jeden wpis wlicza się do dwóch celów naraz")
    void shouldCountSameLogInTwoGoals() {
        Goal yearly = goal(user, null, GoalMetric.SESSIONS, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        Goal monthly = goal(user, dance, GoalMetric.SESSIONS, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        log(user, dance, LocalDate.of(2026, 3, 15), 60);

        Map<Long, GoalProgressView> rows = progressOf(user);

        assertEquals(1L, rows.get(yearly.getId()).getSessions());
        assertEquals(1L, rows.get(monthly.getId()).getSessions());
    }

    @Test
    @DisplayName("wpisy innego użytkownika nie wliczają się; cel bez wpisów ma zero")
    void shouldIgnoreOtherUsersLogsAndReturnZeroRow() {
        Goal g = goal(user, null, GoalMetric.SESSIONS, LocalDate.of(2026, 1, 1), null);
        log(otherUser, dance, LocalDate.of(2026, 2, 1), 60);

        GoalProgressView row = progressOf(user).get(g.getId());

        assertEquals(0L, row.getSessions());
        assertEquals(0L, row.getMinutes());
    }

    @Test
    @DisplayName("wpis bez czasu trwania liczy się jako sesja, ale nie dodaje minut")
    void shouldCountSessionWithoutDuration() {
        Goal g = goal(user, null, GoalMetric.SESSIONS, LocalDate.of(2026, 1, 1), null);
        log(user, dance, LocalDate.of(2026, 2, 1), null);
        log(user, dance, LocalDate.of(2026, 2, 2), 30);

        GoalProgressView row = progressOf(user).get(g.getId());

        assertEquals(2L, row.getSessions());
        assertEquals(30L, row.getMinutes());
    }

    @Test
    @DisplayName("specyfikacja i zapytanie listowe dają ten sam wynik")
    void shouldKeepSpecificationAndListQueryEquivalent() {
        Goal danceInMarch = goal(user, dance, GoalMetric.MINUTES,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        Goal anythingOpen = goal(user, null, GoalMetric.SESSIONS, LocalDate.of(2026, 1, 1), null);
        log(user, dance, LocalDate.of(2026, 2, 28), 60);   // przed oknem pierwszego celu
        log(user, dance, LocalDate.of(2026, 3, 10), 45);
        log(user, gym, LocalDate.of(2026, 3, 12), 30);     // inna kategoria
        log(user, dance, LocalDate.of(2026, 3, 31), null); // bez czasu trwania
        log(otherUser, dance, LocalDate.of(2026, 3, 15), 90);

        Map<Long, GoalProgressView> rows = progressOf(user);

        for (Goal g : List.of(danceInMarch, anythingOpen)) {
            List<WorkoutLog> matched = workoutLogRepository.findAll(WorkoutLogSpecifications.matchingGoal(g));
            long minutes = matched.stream()
                    .map(WorkoutLog::getDurationMin)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum();

            assertEquals(rows.get(g.getId()).getSessions(), (long) matched.size(), "sesje celu " + g.getId());
            assertEquals(rows.get(g.getId()).getMinutes(), minutes, "minuty celu " + g.getId());
        }
    }

    @Test
    @DisplayName("cel osiągnięty listuje tylko wpisy istniejące w chwili oznaczenia")
    void shouldListOnlyLogsExistingAtAchievementForAchievedGoal() {
        Goal g = goal(user, null, GoalMetric.SESSIONS, LocalDate.of(2026, 1, 1), null);
        log(user, dance, LocalDate.of(2026, 2, 1), 60);   // istniał przed osiągnięciem
        log(user, dance, LocalDate.of(2026, 1, 15), 60);  // dopisany później, z datą wsteczną
        em.flush();

        LocalDateTime achievedAt = LocalDateTime.of(2026, 3, 1, 12, 0);
        g.setAchievedAt(achievedAt);
        g.setAchievedValue(1);
        em.flush();

        // created_at jest updatable = false, więc ani dirty checking, ani JPQL go nie ruszą —
        // ustawiamy wprost w bazie, żeby odtworzyć wpis dodany po zamknięciu celu.
        setCreatedAt(LocalDate.of(2026, 2, 1), achievedAt.minusDays(1));
        setCreatedAt(LocalDate.of(2026, 1, 15), achievedAt.plusDays(1));
        em.clear();

        Goal reloaded = goalRepository.findById(g.getId()).orElseThrow();
        List<WorkoutLog> matched = workoutLogRepository.findAll(WorkoutLogSpecifications.matchingGoal(reloaded));

        assertEquals(1, matched.size());
        assertEquals(LocalDate.of(2026, 2, 1), matched.get(0).getPerformedDate());
    }

    private void setCreatedAt(LocalDate performedDate, LocalDateTime createdAt) {
        em.getEntityManager()
                .createNativeQuery("UPDATE workout_log SET created_at = ?1 WHERE performed_date = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, performedDate)
                .executeUpdate();
    }

}