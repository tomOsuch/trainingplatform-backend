package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDateTime;

public record InvitationCheckResponse (
        String email,
        LocalDateTime expiresAt
){

}
