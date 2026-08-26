package pl.tomaszosuch.trainingplatform_backend.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import pl.tomaszosuch.trainingplatform_backend.config.MailProperties;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailDeliveryException;
import pl.tomaszosuch.trainingplatform_backend.service.EmailService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private static final String SUBJECT = "Zaproszenie do Platformy Treningowej";

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.of("pl"));

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    @Override
    public void sendInvitation(String recipientEmail, String invitationUrl, LocalDateTime expiresAt) {

        String expiry = EXPIRY_FORMAT.format(expiresAt);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(properties.getFrom());
            helper.setTo(recipientEmail);
            helper.setSubject(SUBJECT);
            helper.setText(plainBody(invitationUrl, expiry), htmlBody(invitationUrl, expiry));

            mailSender.send(message);

            log.info("Wysłano zaproszenie na adres {}", recipientEmail);

        } catch (MessagingException | MailException ex) {
            throw new EmailDeliveryException(
                    "Nie udało się wysłać zaproszenia na adres " + recipientEmail, ex);
        }
    }

    private String plainBody(String url, String expiry) {
        return """
                Cześć,

                zapraszamy Cię do Platformy Treningowej — aplikacji do planowania
                i rejestrowania treningów.

                Aby założyć konto, otwórz poniższy link:

                %s

                Link jest ważny do %s. Po tym terminie poproś o nowe zaproszenie.

                Jeśli nie spodziewasz się tej wiadomości, zignoruj ją — bez kliknięcia
                w link nic się nie stanie.
                """.formatted(url, expiry);
    }

    private String htmlBody(String url, String expiry) {
        return """
                <!doctype html>
                <html lang="pl">
                <body style="margin:0;padding:24px;background:#f2f4f5;
                             font-family:-apple-system,Segoe UI,Helvetica,Arial,sans-serif;
                             color:#16232b;line-height:1.6">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                             style="max-width:520px;background:#ffffff;border:1px solid #cfd8dc;
                                    border-radius:4px;padding:32px">
                        <tr><td>
                          <h1 style="margin:0 0 16px;font-size:22px;font-weight:600">
                            Zaproszenie do Platformy Treningowej
                          </h1>
                          <p style="margin:0 0 16px">
                            Zapraszamy Cię do aplikacji do planowania i rejestrowania treningów.
                          </p>
                          <p style="margin:0 0 24px">
                            <a href="%s"
                               style="display:inline-block;background:#8a5d14;color:#ffffff;
                                      text-decoration:none;padding:12px 22px;border-radius:3px;
                                      font-weight:600">Załóż konto</a>
                          </p>
                          <p style="margin:0 0 16px;font-size:14px;color:#5b6b75">
                            Link jest ważny do <strong>%s</strong>. Po tym terminie poproś
                            o nowe zaproszenie.
                          </p>
                          <p style="margin:0 0 8px;font-size:13px;color:#5b6b75">
                            Jeśli przycisk nie działa, skopiuj ten adres do przeglądarki:
                          </p>
                          <p style="margin:0;font-size:12px;color:#5b6b75;word-break:break-all">
                            %s
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(url, expiry, url);
    }
}
