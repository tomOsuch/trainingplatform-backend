package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;

public record TrainingPlanResponse(

    Long id,
    String title,
    Long categoryId,
    String categoryName,
    String categoryColor,
    LocalDate plannedDate,
    LocalTime plannedTime,
    Integer durationMin,
    String notes,
    PlanStatus status
) {

}
