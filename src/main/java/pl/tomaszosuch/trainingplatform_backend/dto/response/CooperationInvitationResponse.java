package pl.tomaszosuch.trainingplatform_backend.dto.response;

import pl.tomaszosuch.trainingplatform_backend.enums.CooperationRole;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;

import java.time.LocalDateTime;

public record CooperationInvitationResponse(

        Long id,
        CooperationRole role,
        Long partnerId,
        String partnerFirstName,
        String partnerLastName,
        String partnerEmail,
        CooperationStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
