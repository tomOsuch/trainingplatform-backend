package pl.tomaszosuch.trainingplatform_backend.dto.response;

public record WorkoutTemplateResponse(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String categoryColor,
        String categoryIconName,
        Integer durationMin
) {
}
