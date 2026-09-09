package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

/**
 * Sprawdza samo zapytanie agregujące na prawdziwym PostgreSQL — granice zakresu, izolację
 * użytkowników i wpisy bez czasu trwania. Mockami tego nie da się przetestować.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("WorkoutStatsQueryTest")
class WorkoutStatsQueryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WorkoutLogRepository workoutLogRepository;

    private User user;
    private User otherUser;
    private WorkoutCategory dance;
    private WorkoutCategory gym;

    @BeforeEach
    void setUp() {
        user = em.persist(user("stats@example.com"));
        otherUser = em.persist(user("other@example.com"));
        dance = em.persist(WorkoutCategory.builder().name("Taniec").color("#9B59B6").build());
        gym = em.persist(WorkoutCategory.builder().name("Siłownia").color("#E67E22").build());
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private void log(User owner, WorkoutCategory category, LocalDate date, Integer minutes) {
        em.persist(WorkoutLog.builder()
                .user(owner).category(category).performedDate(date).durationMin(minutes)
                .build());
    }

    private Map<Long, CategoryStatsView> statsFor(User owner) {
        em.flush();
        return workoutLogRepository.aggregateByCategory(owner.getId(), FROM, TO).stream()
                .collect(Collectors.toMap(CategoryStatsView::getCategoryId, Function.identity()));
    }

    @Test
    @DisplayName("granice zakresu są domknięte, wpisy spoza okresu odpadają")
    void shouldIncludeBothBoundaryDays() {
        log(user, dance, LocalDate.of(2026, 2, 28), 60);  // dzień przed
        log(user, dance, FROM, 45);
        log(user, dance, TO, 30);
        log(user, dance, LocalDate.of(2026, 4, 1), 90);   // dzień po

        CategoryStatsView row = statsFor(user).get(dance.getId());

        assertEquals(2L, row.getSessions());
        assertEquals(75L, row.getMinutes());
    }

    @Test
    @DisplayName("rozbicie sumuje się do wartości łącznych i niesie kolor kategorii")
    void shouldSplitByCategoryWithColor() {
        log(user, dance, LocalDate.of(2026, 3, 5), 90);
        log(user, dance, LocalDate.of(2026, 3, 7), 60);
        log(user, gym, LocalDate.of(2026, 3, 9), 45);

        Map<Long, CategoryStatsView> rows = statsFor(user);

        assertEquals(150L, rows.get(dance.getId()).getMinutes());
        assertEquals(2L, rows.get(dance.getId()).getSessions());
        assertEquals("#9B59B6", rows.get(dance.getId()).getCategoryColor());
        assertEquals(45L, rows.get(gym.getId()).getMinutes());

        long totalMinutes = rows.values().stream().mapToLong(CategoryStatsView::getMinutes).sum();
        long totalSessions = rows.values().stream().mapToLong(CategoryStatsView::getSessions).sum();
        assertEquals(195L, totalMinutes);
        assertEquals(3L, totalSessions);
    }

    @Test
    @DisplayName("wpis bez czasu trwania liczy się jako sesja, ale nie dodaje minut")
    void shouldCountSessionWithoutDuration() {
        log(user, dance, LocalDate.of(2026, 3, 5), null);
        log(user, dance, LocalDate.of(2026, 3, 6), 30);

        CategoryStatsView row = statsFor(user).get(dance.getId());

        assertEquals(2L, row.getSessions());
        assertEquals(30L, row.getMinutes());
    }

    @Test
    @DisplayName("wpisy innego użytkownika nie wchodzą do wyniku")
    void shouldIgnoreOtherUsersLogs() {
        log(otherUser, dance, LocalDate.of(2026, 3, 5), 120);
        log(user, gym, LocalDate.of(2026, 3, 6), 40);

        Map<Long, CategoryStatsView> rows = statsFor(user);

        assertEquals(1, rows.size());
        assertEquals(40L, rows.get(gym.getId()).getMinutes());
    }

    @Test
    @DisplayName("okres bez treningów zwraca pustą listę")
    void shouldReturnEmptyListForPeriodWithoutLogs() {
        log(user, dance, LocalDate.of(2026, 1, 15), 60);
        em.flush();

        assertTrue(workoutLogRepository.aggregateByCategory(user.getId(), FROM, TO).isEmpty());
    }

}