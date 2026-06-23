package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkoutCategoryRequest(
    
    @NotBlank(message = "Name is required and cannot be null.")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters.")
    String name,

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color must be a valid hex code, e.g., #FFFFFF or #FFF.")
    String color,

    @Size(max = 255, message = "Icon name must be at most 255 characters.")
    String iconName
) {

}
