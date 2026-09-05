package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.util.List;

public record GoalDetailsResponse(
        GoalResponse goal,
        List<GoalLogEntryResponse> entries
) {
}
