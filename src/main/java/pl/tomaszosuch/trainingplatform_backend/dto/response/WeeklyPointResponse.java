package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDate;

public record WeeklyPointResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        boolean partial,
        long workoutCount,
        long totalMinutes
) {
}
