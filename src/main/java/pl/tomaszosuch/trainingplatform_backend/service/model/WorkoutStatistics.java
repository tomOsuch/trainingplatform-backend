package pl.tomaszosuch.trainingplatform_backend.service.model;

import java.util.List;

public record WorkoutStatistics(
        long totalSessions,
        long totalMinutes,
        List<CategoryStatistics> byCategory
) {
}
