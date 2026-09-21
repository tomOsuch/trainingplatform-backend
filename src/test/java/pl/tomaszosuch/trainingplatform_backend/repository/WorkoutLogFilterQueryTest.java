package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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

import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("WorkoutLogFilterQueryTest")
class WorkoutLogFilterQueryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final LocalDate FROM = LocalDate.of(2026, 3, 10);
    private static final LocalDate TO = LocalDate.of(2026, 3, 20);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WorkoutLogRepository workoutLogRepository;

    private User owner;
    private User otherUser;
    private WorkoutCategory dance;
    private WorkoutCategory gym;

    @BeforeEach
    void setUp() {
        owner = em.persist(user("wlasciciel@example.com"));
        otherUser = em.persist(user("obcy@example.com"));
        dance = em.persist(WorkoutCategory.builder()
                .name("Taniec testowy").color("#9B59B6").iconName("music").build());
        gym = em.persist(WorkoutCategory.builder()
                .name("Siłownia testowa").color("#E67E22").iconName("dumbbell").build());
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private void log(User owner, WorkoutCategory category, LocalDate date) {
        em.persist(WorkoutLog.builder()
                .user(owner).category(category).performedDate(date).durationMin(60)
                .build());
    }

    @Test
    @DisplayName("kategoria i zakres dat zawężają razem, nie wykluczają się")
    void shouldCombineCategoryAndDateRange() {
        log(owner, dance, LocalDate.of(2026, 3, 15));
        log(owner, dance, LocalDate.of(2026, 2, 15));
        log(owner, gym, LocalDate.of(2026, 3, 15));
        em.flush();

        List<WorkoutLog> result = workoutLogRepository.findFiltered(
                owner.getId(), dance.getId(), FROM, TO);

        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 3, 15), result.get(0).getPerformedDate());
    }

    @Test
    @DisplayName("granice zakresu są domknięte obustronnie")
    void shouldIncludeBothBoundaryDays() {
        log(owner, dance, FROM);
        log(owner, dance, TO);
        log(owner, dance, FROM.minusDays(1));
        log(owner, dance, TO.plusDays(1));
        em.flush();

        assertEquals(2, workoutLogRepository.findFiltered(owner.getId(), null, FROM, TO).size());
    }

    @Test
    @DisplayName("pominięte parametry nie zawężają niczego")
    void shouldIgnoreNullParameters() {
        log(owner, dance, LocalDate.of(2026, 1, 5));
        log(owner, gym, LocalDate.of(2026, 6, 20));
        em.flush();

        assertEquals(2, workoutLogRepository.findFiltered(owner.getId(), null, null, null).size());
    }

    @Test
    @DisplayName("jedna granica działa jako przedział otwarty")
    void shouldSupportOpenEndedRange() {
        log(owner, dance, LocalDate.of(2026, 3, 1));
        log(owner, dance, LocalDate.of(2026, 3, 25));
        em.flush();

        assertEquals(1, workoutLogRepository.findFiltered(owner.getId(), null, FROM, null).size());
        assertEquals(1, workoutLogRepository.findFiltered(owner.getId(), null, null, FROM).size());
    }

    @Test
    @DisplayName("filtr nie wychodzi poza wpisy właściciela")
    void shouldNotLeakOtherUsersLogs() {
        log(owner, dance, LocalDate.of(2026, 3, 15));
        log(otherUser, dance, LocalDate.of(2026, 3, 15));
        em.flush();

        List<WorkoutLog> result = workoutLogRepository.findFiltered(
                owner.getId(), dance.getId(), FROM, TO);

        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(l -> l.getUser().getId().equals(owner.getId())));
    }

    @Test
    @DisplayName("wynik jest posortowany od najnowszego")
    void shouldSortNewestFirst() {
        log(owner, dance, LocalDate.of(2026, 3, 12));
        log(owner, dance, LocalDate.of(2026, 3, 18));
        em.flush();

        List<WorkoutLog> result = workoutLogRepository.findFiltered(owner.getId(), null, FROM, TO);

        assertEquals(LocalDate.of(2026, 3, 18), result.get(0).getPerformedDate());
    }
}