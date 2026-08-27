package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.time.LocalDateTime;

public record PasswordResetCheckResponse(

        String email,
        LocalDateTime expiresAt
) {

}
