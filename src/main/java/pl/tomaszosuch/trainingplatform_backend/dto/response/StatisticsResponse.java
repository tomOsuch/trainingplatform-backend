package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDate;
import java.util.List;

public record StatisticsResponse(
        LocalDate from,
        LocalDate to,
        long workoutCount,
        long totalMinutes,
        List<CategoryStatisticsResponse> byCategory,
        PlanCompletionResponse planCompletion
) {
}
