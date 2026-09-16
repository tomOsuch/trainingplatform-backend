package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pl.tomaszosuch.trainingplatform_backend.enums.CategoryIcon;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("DefaultCategoriesSeedTest")
class DefaultCategoriesSeedTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static final String MIGRATION_PATH = "db/migration/V14__default_workout_categories.sql";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("świeża baza ma trzy domyślne kategorie z umówionym kolorem i ikoną")
    void shouldSeedThreeDefaultCategories() {
        assertEquals(3, countAll());

        assertCategory("Taniec", "#9B59B6", "music");
        assertCategory("Gimnastyka", "#E74C3C", "person-standing");
        assertCategory("Ogólnorozwojowy", "#27AE60", "dumbbell");
    }

    @Test
    @DisplayName("ponowne wykonanie migracji nie duplikuje kategorii")
    void shouldNotDuplicateOnSecondRun() throws IOException {
        jdbcTemplate.execute(migrationSql());

        assertEquals(3, countAll());
        assertEquals(1, countByName("Taniec"));
    }

    @Test
    @DisplayName("migracja nie nadpisuje kategorii, która już istnieje pod tą nazwą")
    void shouldNotOverwriteExistingCategory() throws IOException {
        jdbcTemplate.update("UPDATE workout_category SET color = '#123456' WHERE name = 'Taniec'");

        jdbcTemplate.execute(migrationSql());

        assertEquals("#123456", jdbcTemplate.queryForObject(
                "SELECT color FROM workout_category WHERE name = 'Taniec'", String.class));
        assertEquals(1, countByName("Taniec"));
    }

    private void assertCategory(String name, String color, String iconName) {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT color, icon_name FROM workout_category WHERE name = ?", name);

        assertEquals(color, row.get("color"));
        assertEquals(iconName, row.get("icon_name"));
        assertTrue(CategoryIcon.allowedNames().contains(iconName),
                "Ikona " + iconName + " nie należy do CategoryIcon");
    }

    private int countAll() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM workout_category", Integer.class);
    }

    private int countByName(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workout_category WHERE name = ?", Integer.class, name);
    }

    private static String migrationSql() throws IOException {
        return new ClassPathResource(MIGRATION_PATH).getContentAsString(StandardCharsets.UTF_8);
    }
}