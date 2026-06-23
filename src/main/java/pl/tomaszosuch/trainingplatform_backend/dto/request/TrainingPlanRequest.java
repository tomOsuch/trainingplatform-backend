package pl.tomaszosuch.trainingplatform_backend.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TrainingPlanRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    String title,
    @NotNull(message = "Category ID is required")
    Long categoryId,
    @NotNull(message = "Planned date is required")
    @FutureOrPresent(message = "Planned date must be in the future or present")
    LocalDate plannedDate,
    LocalTime plannedTime,
    @Positive(message = "Duration must be a positive number")
    Integer durationMin,
    String notes
) {

}
