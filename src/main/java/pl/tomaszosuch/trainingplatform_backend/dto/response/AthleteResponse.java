package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDateTime;

public record AthleteResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        LocalDateTime cooperationSince
) {
}
