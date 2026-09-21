package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.repository.CooperationRepository;
import pl.tomaszosuch.trainingplatform_backend.security.AthleteAccessGuard;

@ExtendWith(MockitoExtension.class)
@DisplayName("AthleteAccessGuardTest")
class AthleteAccessGuardTest {

    private static final Long COACH_ID = 1L;
    private static final Long ATHLETE_ID = 2L;

    @Mock
    private CooperationRepository cooperationRepository;

    @InjectMocks
    private AthleteAccessGuard guard;

    @Test
    @DisplayName("aktywna współpraca otwiera dostęp")
    void shouldAllowWhenActive() {
        when(cooperationRepository.existsByCoachIdAndAthleteIdAndStatus(
                COACH_ID, ATHLETE_ID, CooperationStatus.ACTIVE)).thenReturn(true);

        assertDoesNotThrow(() -> guard.requireActiveCooperation(COACH_ID, ATHLETE_ID));
    }

    @Test
    @DisplayName("po zakończeniu współpracy dostęp znika natychmiast")
    void shouldDenyAfterCooperationEnded() {
        when(cooperationRepository.existsByCoachIdAndAthleteIdAndStatus(
                COACH_ID, ATHLETE_ID, CooperationStatus.ACTIVE)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> guard.requireActiveCooperation(COACH_ID, ATHLETE_ID));
    }
}