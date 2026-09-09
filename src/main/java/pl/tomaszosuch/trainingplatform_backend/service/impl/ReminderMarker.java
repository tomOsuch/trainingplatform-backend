package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReminderMarker {

    private final TrainingPlanRepository trainingPlanRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long planId, LocalDateTime sentAt) {
        return trainingPlanRepository.markReminderSent(planId, sentAt) == 1;
    }
}
