package pl.tomaszosuch.trainingplatform_backend.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TrainingPlanRequest(
        @NotBlank(message = "Tytuł jest wymagany") @Size(max = 200, message = "Tytuł może mieć maksymalnie 200 znaków") String title,
        @NotNull(message = "Kategoria jest wymagana") Long categoryId,
        @NotNull(message = "Data treningu jest wymagana") LocalDate plannedDate,
        LocalTime plannedTime,
        @Positive(message = "Czas trwania musi być większy od 0") Integer durationMin,
        String notes) {

}