package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDate;

public record GoalLogEntryResponse(
        Long id,
        String title,
        LocalDate performedDate,
        Integer durationMin,
        Long categoryId,
        String categoryName,
        String categoryColor
) {
}
