package pl.tomaszosuch.trainingplatform_backend.dto.response;

import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalResponse(
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
        boolean achieved,
        LocalDateTime achievedAt
) {
}
