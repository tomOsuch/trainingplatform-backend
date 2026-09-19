package pl.tomaszosuch.trainingplatform_backend.dto.response;

import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;

import java.time.LocalDateTime;

public record CooperationInvitationResponse(

        Long id,
        Long coachId,
        String coachFirstName,
        String coachLastName,
        String coachEmail,
        CooperationStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
