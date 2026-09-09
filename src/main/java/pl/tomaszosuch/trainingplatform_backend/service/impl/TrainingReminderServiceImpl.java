package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.config.RemindersProperties;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailDeliveryException;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;
import pl.tomaszosuch.trainingplatform_backend.service.EmailService;
import pl.tomaszosuch.trainingplatform_backend.service.TrainingReminderService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingReminderServiceImpl implements TrainingReminderService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final ReminderMarker reminderMarker;
    private final EmailService emailService;
    private final RemindersProperties properties;

    @Override
    public int sendDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<TrainingPlan> candidates = readCandidates(now);

        int sent = 0;
        for (TrainingPlan plan : candidates) {
            if (!isDue(plan, now)) {
                continue;
            }
            if (!reminderMarker.claim(plan.getId(), now)) {
                continue;
            }
            if (send(plan)) {
                sent++;
            }
        }
        return sent;
    }

    @Transactional(readOnly = true)
    protected List<TrainingPlan> readCandidates(LocalDateTime now) {
        return trainingPlanRepository.findReminderCandidates(now.toLocalDate());
    }

    private boolean send(TrainingPlan plan) {
        User user = plan.getUser();
        try {
            emailService.sendTrainingReminder(
                    user.getEmail(),
                    plan.getTitle(),
                    plan.getCategory().getName(),
                    plan.getPlannedDate(),
                    plan.getPlannedTime());
            return true;
        } catch (EmailDeliveryException ex) {
            log.error("Nie udało się wysłać przypomnienia o planie {} na adres {}",
                    plan.getId(), user.getEmail(), ex);
            return false;
        }
    }

    private boolean isDue(TrainingPlan plan, LocalDateTime now) {
        LocalDateTime plannedAt = plannedAt(plan);

        if (!plannedAt.isAfter(now)) {
            return false;
        }

        LocalDateTime reminderAt = plannedAt.minusHours(plan.getUser().getReminderHoursBefore());
        return !reminderAt.isAfter(now);
    }

    private LocalDateTime plannedAt(TrainingPlan plan) {
        return LocalDateTime.of(
                plan.getPlannedDate(),
                plan.getPlannedTime() != null ? plan.getPlannedTime() : properties.getDefaultStartTime());
    }
}
