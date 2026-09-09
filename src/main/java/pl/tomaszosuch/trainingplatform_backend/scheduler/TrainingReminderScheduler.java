package pl.tomaszosuch.trainingplatform_backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.tomaszosuch.trainingplatform_backend.service.TrainingReminderService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.reminders.enabled", havingValue = "true")
public class TrainingReminderScheduler {

    private final TrainingReminderService trainingReminderService;

    @Scheduled(cron = "${app.reminders.cron}")
    public void sendDueReminders() {
        int sent = trainingReminderService.sendDueReminders();
        if (sent > 0) {
            log.info("Wysłano {} przypomnień o zaplanowanych treningach", sent);
        }
    }
}
