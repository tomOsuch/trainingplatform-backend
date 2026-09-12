package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pl.tomaszosuch.trainingplatform_backend.enums.CategoryIcon;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("WorkoutCategoryIconConstraintTest")
class WorkoutCategoryIconConstraintTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final String INSERT_WITH_ICON =
            "INSERT INTO workout_category (name, color, icon_name) VALUES (?, '#000000', ?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("baza przyjmuje każdą nazwę z zestawu w kodzie")
    void shouldAcceptEveryIconFromCode() {
        for (CategoryIcon icon : CategoryIcon.values()) {
            jdbcTemplate.update(INSERT_WITH_ICON, "Kategoria " + icon.name(), icon.value());
        }

        Integer inserted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workout_category WHERE name LIKE 'Kategoria %'", Integer.class);

        assertEquals(CategoryIcon.values().length, inserted);
    }

    @Test
    @DisplayName("baza odrzuca nazwę spoza zestawu")
    void shouldRejectUnknownIcon() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT_WITH_ICON, "Taniec ludowy", "dance"));
    }

    @Test
    @DisplayName("baza odrzuca brak ikony wstawiony wprost jako NULL")
    void shouldRejectNullIcon() {
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(INSERT_WITH_ICON, "Bez ikony", (Object) null));
    }

    @Test
    @DisplayName("wstawienie bez kolumny icon_name bierze wartość domyślną")
    void shouldApplyDefaultIcon() {
        jdbcTemplate.update("INSERT INTO workout_category (name) VALUES ('Z domyślną')");

        assertEquals(CategoryIcon.DEFAULT.value(), jdbcTemplate.queryForObject(
                "SELECT icon_name FROM workout_category WHERE name = 'Z domyślną'", String.class));
    }
}