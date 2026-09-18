package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("CooperationRepositoryTest")
class CooperationRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final String INSERT = """
            INSERT INTO cooperation (coach_id, athlete_id, status, created_at)
            VALUES (?, ?, ?, now())
            """;

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CooperationRepository cooperationRepository;

    private User coach;
    private User athlete;
    private User outsider;

    @BeforeEach
    void setUp() {
        coach = em.persist(user("trener@example.com"));
        athlete = em.persist(user("podopieczny@example.com"));
        outsider = em.persist(user("obcy@example.com"));
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private Cooperation cooperation(User coach, User athlete, CooperationStatus status) {
        return em.persist(Cooperation.builder()
                .coach(coach).athlete(athlete).status(status)
                .build());
    }

    @Test
    @DisplayName("nie można być trenerem samego siebie")
    void shouldRejectSelfCooperation() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT, coach.getId(), coach.getId(), "ACTIVE"));
    }

    @Test
    @DisplayName("para kont nie może mieć drugiej otwartej współpracy")
    void shouldRejectSecondOpenCooperationForSamePair() {
        cooperation(coach, athlete, CooperationStatus.ACTIVE);
        em.flush();

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT, coach.getId(), athlete.getId(), "PENDING"));
    }

    @Test
    @DisplayName("po zakończeniu współpracy można zaprosić tę samą osobę ponownie")
    void shouldAllowNewCooperationAfterPreviousEnded() {
        cooperation(coach, athlete, CooperationStatus.ENDED);
        cooperation(coach, athlete, CooperationStatus.REJECTED);
        em.flush();

        jdbcTemplate.update(INSERT, coach.getId(), athlete.getId(), "PENDING");

        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cooperation WHERE coach_id = ?", Integer.class, coach.getId()));
    }

    @Test
    @DisplayName("dwie osoby mogą prowadzić się nawzajem")
    void shouldAllowReversedRoles() {
        cooperation(coach, athlete, CooperationStatus.ACTIVE);
        em.flush();

        jdbcTemplate.update(INSERT, athlete.getId(), coach.getId(), "ACTIVE");

        assertTrue(cooperationRepository.existsByCoachIdAndAthleteIdAndStatus(
                coach.getId(), athlete.getId(), CooperationStatus.ACTIVE));
        assertTrue(cooperationRepository.existsByCoachIdAndAthleteIdAndStatus(
                athlete.getId(), coach.getId(), CooperationStatus.ACTIVE));
    }

    @Test
    @DisplayName("usunięcie konta trenera kasuje relację")
    void shouldCascadeDeleteWhenCoachDeleted() {
        cooperation(coach, athlete, CooperationStatus.ACTIVE);
        em.flush();
        em.clear();

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", coach.getId());

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cooperation", Integer.class));
    }

    @Test
    @DisplayName("usunięcie konta podopiecznego kasuje relację")
    void shouldCascadeDeleteWhenAthleteDeleted() {
        cooperation(coach, athlete, CooperationStatus.ACTIVE);
        em.flush();
        em.clear();

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", athlete.getId());

        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cooperation", Integer.class));
    }

    @Test
    @DisplayName("oba kierunki pytania trafiają na właściwą stronę relacji")
    void shouldAnswerBothDirectionalQuestions() {
        cooperation(coach, athlete, CooperationStatus.ACTIVE);
        cooperation(coach, outsider, CooperationStatus.PENDING);
        em.flush();

        assertEquals(1, cooperationRepository
                .findByCoachIdAndStatus(coach.getId(), CooperationStatus.ACTIVE).size());
        assertEquals(1, cooperationRepository
                .findByAthleteIdAndStatus(athlete.getId(), CooperationStatus.ACTIVE).size());

        assertTrue(cooperationRepository
                .findByCoachIdAndStatus(athlete.getId(), CooperationStatus.ACTIVE).isEmpty());
        assertFalse(cooperationRepository.existsByCoachIdAndAthleteIdAndStatus(
                coach.getId(), outsider.getId(), CooperationStatus.ACTIVE));
    }
}