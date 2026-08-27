package pl.tomaszosuch.trainingplatform_backend.service;

import java.time.LocalDateTime;

public interface EmailService {

    void sendInvitation(String recipientEmail, String invitationUrl, LocalDateTime expiresAt);

    void sendPasswordReset(String recipientEmail, String resetUrl, LocalDateTime expiresAt);
}
