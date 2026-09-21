package pl.tomaszosuch.trainingplatform_backend.dto.response;

import pl.tomaszosuch.trainingplatform_backend.enums.CooperationRole;

import java.time.LocalDateTime;

public record CooperationResponse(
        Long id,
        CooperationRole role,
        Long partnerId,
        String partnerFirstName,
        String partnerLastName,
        String partnerEmail,
        LocalDateTime since
) {
}
