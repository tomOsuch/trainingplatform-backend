package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record WorkoutTemplateRequest(

        @NotBlank(message = "Nazwa szablonu jest wymagana")
        @Size(max = 200, message = "Nazwa może mieć maksymalnie 200 znaków")
        String name,

        String description,

        @NotNull(message = "Kategoria jest wymagana")
        Long categoryId,

        @Positive(message = "Czas trwania musi być większy od 0")
        Integer durationMin
) {
}
