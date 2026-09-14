package pl.tomaszosuch.trainingplatform_backend.dto.response;

public record CategoryStatisticsResponse(
        Long categoryId,
        String categoryName,
        String categoryColor,
        String categoryIconName,
        long workoutCount,
        long totalMinutes
) {
}
