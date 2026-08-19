package pl.tomaszosuch.trainingplatform_backend.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

public record WorkoutLogRequest(

        @NotNull(message = "Kategoria jest wymagana") Long categoryId,

        Long planId,

        @NotNull(message = "Data treningu jest wymagana") @PastOrPresent(message = "Data treningu nie może być z przyszłości") LocalDate performedDate,

        LocalTime performedTime,

        @Positive(message = "Czas trwania musi być większy od 0") Integer durationMin,

        @Min(value = 1, message = "Intensywność musi być od 1 do 10") @Max(value = 10, message = "Intensywność musi być od 1 do 10") Integer intensity,

        String notes

) {

}
