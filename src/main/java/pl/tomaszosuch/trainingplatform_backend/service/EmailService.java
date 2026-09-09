package pl.tomaszosuch.trainingplatform_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface EmailService {

    void sendInvitation(String recipientEmail, String invitationUrl, LocalDateTime expiresAt);

    void sendPasswordReset(String recipientEmail, String resetUrl, LocalDateTime expiresAt);

    void sendTrainingReminder(String recipientEmail, String planTitle, String categoryName,
                              LocalDate plannedDate, LocalTime plannedTime);
}
