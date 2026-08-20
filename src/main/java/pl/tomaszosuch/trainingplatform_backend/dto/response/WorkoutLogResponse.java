package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record WorkoutLogResponse(
        Long id,
        String title,
        Long categoryId,
        String categoryName,
        String categoryColor,
        Long planId,
        LocalDate performedDate,
        LocalTime performedTime,
        Integer durationMin,
        Integer intensity,
        String notes) {

}
