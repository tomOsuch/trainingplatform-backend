package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.tomaszosuch.trainingplatform_backend.validation.ValidIconName;

public record WorkoutCategoryRequest(

        @NotBlank(message = "Nazwa kategorii jest wymagana")
        @Size(min = 1, max = 100, message = "Nazwa musi mieć od 1 do 100 znaków")
        String name,

        @NotBlank(message = "Kolor kategorii jest wymagany")
        @Pattern(regexp = "^#[A-Fa-f0-9]{6}$", message = "Kolor musi być pełnym kodem HEX, np. #9B59B6")
        String color,

        @NotBlank(message = "Ikona kategorii jest wymagana")
        @ValidIconName
        String iconName
) {

}