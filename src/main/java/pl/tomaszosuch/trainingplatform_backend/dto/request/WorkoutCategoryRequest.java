package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkoutCategoryRequest(

        @NotBlank(message = "Nazwa kategorii jest wymagana")
        @Size(min = 1, max = 100, message = "Nazwa musi mieć od 1 do 100 znaków")
        String name,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Kolor musi być poprawnym kodem HEX, np. #FFFFFF lub #FFF")
        String color,

        @Size(max = 255, message = "Nazwa ikony może mieć maksymalnie 255 znaków")
        String iconName
) {

}