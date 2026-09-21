package pl.tomaszosuch.trainingplatform_backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.repository.CooperationRepository;

@Component
@RequiredArgsConstructor
public class AthleteAccessGuard {

    private static final String NO_COOPERATION_MESSAGE = "Nie prowadzisz tej osoby";

    private final CooperationRepository cooperationRepository;

    public void requireActiveCooperation(Long coachId, Long athleteId) {
        if (!cooperationRepository.existsByCoachIdAndAthleteIdAndStatus(
                coachId, athleteId, CooperationStatus.ACTIVE)) {
            throw new AccessDeniedException(NO_COOPERATION_MESSAGE);
        }
    }
}
