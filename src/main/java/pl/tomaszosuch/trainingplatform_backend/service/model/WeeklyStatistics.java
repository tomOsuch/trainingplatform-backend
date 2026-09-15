package pl.tomaszosuch.trainingplatform_backend.service.model;

import java.util.List;

public record WeeklyStatistics(
        long totalSessions,
        long totalMinutes,
        List<WeeklyPoint> weeks
) {
}
