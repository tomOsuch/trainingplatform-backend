package pl.tomaszosuch.trainingplatform_backend.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;

@DisplayName("TrainingPlanMapperTest")
class TrainingPlanMapperTest {

    private static final Long OWNER_ID = 2L;
    private static final Long COACH_ID = 7L;

    private final TrainingPlanMapper mapper = new TrainingPlanMapperImpl();

    private static TrainingPlan plan(User author) {
        return TrainingPlan.builder()
                .id(10L)
                .user(User.builder().id(OWNER_ID).firstName("Anna").lastName("Nowak").build())
                .createdBy(author)
                .category(WorkoutCategory.builder()
                        .id(5L).name("Taniec").color("#9B59B6").iconName("music")
                        .build())
                .title("Interwały")
                .plannedDate(LocalDate.of(2026, 10, 1))
                .status(PlanStatus.PLANNED)
                .build();
    }

    @Test
    @DisplayName("plan od trenera: trzy pola autorstwa wypełnione i opisują tę samą osobę")
    void shouldMapAllThreeAuthorFieldsForCoachPlan() {
        User coach = User.builder().id(COACH_ID).firstName("Jan").lastName("Kowalski").build();

        TrainingPlanResponse response = mapper.toResponse(plan(coach));

        assertTrue(response.createdByCoach());
        assertEquals("Jan Kowalski", response.createdByName());
        assertEquals(COACH_ID, response.createdById());
    }

    @Test
    @DisplayName("bez autora - plan własny albo konto autora usunięte - trzy pola puste razem")
    void shouldLeaveAllThreeAuthorFieldsEmptyWithoutAuthor() {
        TrainingPlanResponse response = mapper.toResponse(plan(null));

        assertFalse(response.createdByCoach());
        assertNull(response.createdByName());
        assertNull(response.createdById());
    }
}