package pl.tomaszosuch.trainingplatform_backend.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pl.tomaszosuch.trainingplatform_backend.dto.response.StatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.service.model.CategoryStatistics;
import pl.tomaszosuch.trainingplatform_backend.service.model.PlanCompletion;
import pl.tomaszosuch.trainingplatform_backend.service.model.WorkoutStatistics;

@DisplayName("StatisticsMapperTest")
class StatisticsMapperTest {

    private final StatisticsMapper mapper = new StatisticsMapperImpl();

    @Test
    @DisplayName("sesje mapują się na workoutCount, mianownik i procent na pola odpowiedzi")
    void shouldMapNamesAndDerivedFields() {
        WorkoutStatistics stats = new WorkoutStatistics(7, 390,
                List.of(new CategoryStatistics(5L, "Taniec", "#9B59B6", 4L, 240L)));
        PlanCompletion completion = new PlanCompletion(6, 2, 3, 4);

        StatisticsResponse response = mapper.toResponse(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), stats, completion);

        assertEquals(7, response.workoutCount());
        assertEquals(390, response.totalMinutes());
        assertEquals(4, response.byCategory().get(0).workoutCount());
        assertEquals("#9B59B6", response.byCategory().get(0).categoryColor());
        assertEquals(12, response.planCompletion().completionBase());
        assertEquals(50, response.planCompletion().completionRate());
    }

    @Test
    @DisplayName("pusty okres: procent null zamiast zera")
    void shouldMapNullRate() {
        StatisticsResponse response = mapper.toResponse(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                new WorkoutStatistics(0, 0, List.of()), new PlanCompletion(0, 0, 0, 0));

        assertNull(response.planCompletion().completionRate());
    }

}