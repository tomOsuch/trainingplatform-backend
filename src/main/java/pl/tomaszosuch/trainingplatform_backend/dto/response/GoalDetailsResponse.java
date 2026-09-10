package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;

public record GoalDetailsResponse(
        Long id,
        String title,
        String description,
        Long categoryId,
        String categoryName,
        String categoryColor,
        GoalMetric metric,
        Integer targetValue,
        LocalDate startDate,
        LocalDate endDate,
        long currentValue,
        int percent,
        boolean targetReached,
        boolean achieved,
        LocalDateTime achievedAt,
        List<GoalLogEntryResponse> entries) {

}