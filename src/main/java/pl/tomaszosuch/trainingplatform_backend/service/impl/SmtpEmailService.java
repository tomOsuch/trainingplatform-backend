package pl.tomaszosuch.trainingplatform_backend.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.tomaszosuch.trainingplatform_backend.config.MailProperties;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailDeliveryException;
import pl.tomaszosuch.trainingplatform_backend.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private static final Locale PL = Locale.of("pl");

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", PL);

    private final JavaMailSender mailSender;
    private final MailProperties properties;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void sendInvitation(String recipientEmail, String invitationUrl, LocalDateTime expiresAt) {

        String expiry = EXPIRY_FORMAT.format(expiresAt);

        send(recipientEmail,
                "Zaproszenie do Platformy Treningowej",
                "mail/invitation",
                Map.of("url", invitationUrl, "expiry", expiry),
                """
                Cześć,
   
                zapraszamy Cię do Platformy Treningowej — aplikacji do planowania
                i rejestrowania treningów.
   
                Aby założyć konto, otwórz poniższy link:
   
                %s
   
                Link jest ważny do %s. Po tym terminie poproś o nowe zaproszenie.
   
                Jeśli nie spodziewasz się tej wiadomości, zignoruj ją — bez kliknięcia
                w link nic się nie stanie.
                """.formatted(invitationUrl, expiry));
    }

    @Override
    public void sendPasswordReset(String recipientEmail, String resetUrl, LocalDateTime expiresAt) {

        String expiry = EXPIRY_FORMAT.format(expiresAt);

        send(recipientEmail,
                "Resetowanie hasła — Platforma Treningowa",
                "mail/password-reset",
                Map.of("url", resetUrl, "expiry", expiry),
                """
                Cześć,
   
                ktoś poprosił o zresetowanie hasła do Twojego konta w Platformie Treningowej.
   
                Aby ustawić nowe hasło, otwórz poniższy link:
   
                %s
   
                Link jest ważny do %s i zadziała tylko raz.
   
                Jeśli to nie Ty prosiłeś o reset, zignoruj tę wiadomość. Twoje hasło
                pozostanie bez zmian.
                """.formatted(resetUrl, expiry));
    }

    private void send(String recipient, String subject, String template,
                      Map<String, Object> variables, String plainText) {
        try {
            Context context = new Context(PL, variables);
            String html = templateEngine.process(template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(properties.getFrom());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(plainText, html);

            mailSender.send(message);

            log.info("Wysłano wiadomość „{}” na adres {}", subject, recipient);

        } catch (MessagingException | MailException ex) {
            throw new EmailDeliveryException(
                    "Nie udało się wysłać wiadomości na adres " + recipient, ex);
        }
    }
}