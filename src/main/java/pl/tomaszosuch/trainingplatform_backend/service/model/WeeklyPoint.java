package pl.tomaszosuch.trainingplatform_backend.service.model;

import java.time.LocalDate;

public record WeeklyPoint(
        LocalDate weekStart,
        LocalDate weekEnd,
        boolean partial,
        long sessions,
        long minutes
) {
}
