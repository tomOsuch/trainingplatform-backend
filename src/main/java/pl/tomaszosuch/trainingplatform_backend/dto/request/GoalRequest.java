package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;

import java.time.LocalDate;

public record GoalRequest(
        @NotBlank(message = "Tytuł jest wymagany")
        @Size(max = 200, message = "Tytuł może mieć maksymalnie 200 znaków")
        String title,
        String description,
        Long categoryId,
        @NotNull(message = "Miara celu jest wymagana")
        GoalMetric metric,
        @NotNull(message = "Wartość docelowa jest wymagana")
        @Positive(message = "Wartość docelowa musi być większa od 0")
        Integer targetValue,
        LocalDate startDate,
        LocalDate endDate
) {

}
