package pl.tomaszosuch.trainingplatform_backend.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;
import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;

@DisplayName("GoalMapperTest")
class GoalMapperTest {

    private final GoalMapper mapper = new GoalMapperImpl();

    private static Goal goal(WorkoutCategory category) {
        return Goal.builder()
                .id(10L)
                .title("100 godzin tańca")
                .category(category)
                .metric(GoalMetric.MINUTES)
                .targetValue(6000)
                .startDate(LocalDate.of(2026, 1, 1))
                .build();
    }

    @Test
    @DisplayName("cel z kategorią niesie nazwę, kolor i ikonę")
    void shouldMapAllThreeCategoryAttributes() {
        WorkoutCategory category = WorkoutCategory.builder()
                .id(5L).name("Taniec").color("#9B59B6").iconName("music")
                .build();

        GoalResponse response = mapper.toResponse(goal(category), new GoalProgress(1500, 6000));

        assertEquals(5L, response.categoryId());
        assertEquals("Taniec", response.categoryName());
        assertEquals("#9B59B6", response.categoryColor());
        assertEquals("music", response.categoryIconName());
    }

    @Test
    @DisplayName("cel bez kategorii zwraca null we wszystkich polach kategorii")
    void shouldReturnNullCategoryFieldsConsistently() {
        GoalResponse response = mapper.toResponse(goal(null), new GoalProgress(0, 6000));

        // Spójność, nie samo pole: gdyby ikona wracała jako null przy wypełnionej nazwie
        // (albo odwrotnie), front narysowałby pigułkę kategorii w połowie.
        assertNull(response.categoryId());
        assertNull(response.categoryName());
        assertNull(response.categoryColor());
        assertNull(response.categoryIconName());
    }
}
