package pl.tomaszosuch.trainingplatform_backend.dto.response;

public record WorkoutCategoryResponse(
    Long id,
    String name,
    String color,
    String iconName
) {

}
