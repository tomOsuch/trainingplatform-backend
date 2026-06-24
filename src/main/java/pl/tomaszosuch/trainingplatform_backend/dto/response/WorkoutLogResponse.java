package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDate;

public record WorkoutLogResponse(
    Long id,
    Long categoryId,
    String categoryName,
    String categoryColor,
    Long planId,
    LocalDate performedDate,
    Integer durationMin,
    Integer intensity,
    String notes
) {

}
