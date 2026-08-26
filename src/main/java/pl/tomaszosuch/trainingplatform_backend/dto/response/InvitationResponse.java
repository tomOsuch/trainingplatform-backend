package pl.tomaszosuch.trainingplatform_backend.dto.response;

import pl.tomaszosuch.trainingplatform_backend.enums.InvitationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

import java.time.LocalDateTime;

public record InvitationResponse(
        Long id,
        String email,
        Role role,
        InvitationStatus status,
        String invitedByEmail,
        LocalDateTime expiresAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {

}
