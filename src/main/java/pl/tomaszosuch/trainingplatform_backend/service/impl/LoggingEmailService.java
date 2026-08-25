package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pl.tomaszosuch.trainingplatform_backend.service.EmailService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.of("pl"));

    @Override
    public void sendInvitation(String recipientEmail, String invitationUrl, LocalDateTime expiresAt) {
        log.info("""

                ─────────── ZAPROSZENIE — tryb lokalny, mail NIE został wysłany ───────────
                  Do:        {}
                  Link:      {}
                  Ważne do:  {}
                ──────────────────────────────────────────────────────────────────────────
                """,
                recipientEmail, invitationUrl, EXPIRY_FORMAT.format(expiresAt));
    }
}
