package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import pl.tomaszosuch.trainingplatform_backend.dto.request.TrainingPlanRequest;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.mapper.CooperationMapper;
import pl.tomaszosuch.trainingplatform_backend.mapper.CooperationMapperImpl;
import pl.tomaszosuch.trainingplatform_backend.repository.CooperationRepository;
import pl.tomaszosuch.trainingplatform_backend.security.AthleteAccessGuard;
import pl.tomaszosuch.trainingplatform_backend.service.impl.CoachAthleteServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoachAthleteServiceImplTest")
class CoachAthleteServiceImplTest {

    private static final Long COACH_ID = 1L;
    private static final Long ATHLETE_ID = 2L;
    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);
    private static final Long PLAN_ID = 30L;
    private static final TrainingPlanRequest REQUEST = new TrainingPlanRequest(
            "Interwały", 5L, LocalDate.of(2026, 3, 10), null, 45, null);

    @Mock
    private AthleteAccessGuard athleteAccessGuard;

    @Mock
    private CooperationRepository cooperationRepository;

    @Mock
    private TrainingPlanService trainingPlanService;

    @Mock
    private WorkoutLogService workoutLogService;

    @Mock
    private GoalService goalService;

    @Mock
    private StatisticsService statisticsService;

    private CoachAthleteServiceImpl service;

    @BeforeEach
    void setUp() {
        CooperationMapper mapper = new CooperationMapperImpl();
        service = new CoachAthleteServiceImpl(athleteAccessGuard, cooperationRepository, mapper,
                trainingPlanService, workoutLogService, goalService, statisticsService);
    }

    private void givenNoCooperation() {
        doThrow(new AccessDeniedException("Nie prowadzisz tej osoby"))
                .when(athleteAccessGuard).requireActiveCooperation(COACH_ID, ATHLETE_ID);
    }

    @Test
    @DisplayName("dane podopiecznego pobierane są JEGO identyfikatorem, nie trenera")
    void shouldQueryWithAthleteId() {
        when(trainingPlanService.getTrainingPlansByUserId(ATHLETE_ID, FROM, TO))
                .thenReturn(List.of());

        service.trainingPlans(COACH_ID, ATHLETE_ID, FROM, TO);

        verify(trainingPlanService).getTrainingPlansByUserId(ATHLETE_ID, FROM, TO);
        verify(trainingPlanService, never()).getTrainingPlansByUserId(eq(COACH_ID), any(), any());
    }

    @Test
    @DisplayName("brak relacji blokuje plany i nie sięga do serwisu")
    void shouldDenyTrainingPlans() {
        givenNoCooperation();

        assertThrows(AccessDeniedException.class,
                () -> service.trainingPlans(COACH_ID, ATHLETE_ID, FROM, TO));

        verify(trainingPlanService, never()).getTrainingPlansByUserId(anyLong(), any(), any());
    }

    @Test
    @DisplayName("brak relacji blokuje dziennik i nie sięga do serwisu")
    void shouldDenyWorkoutLogs() {
        givenNoCooperation();

        assertThrows(AccessDeniedException.class,
                () -> service.workoutLogs(COACH_ID, ATHLETE_ID, null, FROM, TO));

        verify(workoutLogService, never()).getUserLogs(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("brak relacji blokuje cele i nie sięga do serwisu")
    void shouldDenyGoals() {
        givenNoCooperation();

        assertThrows(AccessDeniedException.class,
                () -> service.goals(COACH_ID, ATHLETE_ID, null));

        verify(goalService, never()).getGoals(anyLong(), any());
    }

    @Test
    @DisplayName("brak relacji blokuje statystyki i agregat tygodniowy")
    void shouldDenyStatistics() {
        givenNoCooperation();

        assertThrows(AccessDeniedException.class,
                () -> service.statistics(COACH_ID, ATHLETE_ID, FROM, TO));
        assertThrows(AccessDeniedException.class,
                () -> service.weeklyStatistics(COACH_ID, ATHLETE_ID, FROM, TO));

        verify(statisticsService, never()).statistics(anyLong(), any(), any());
        verify(statisticsService, never()).weeklyStatistics(anyLong(), any(), any());
    }

    @Test
    @DisplayName("lista podopiecznych nie pyta strażnika — zawęża ją samo zapytanie")
    void shouldNotGuardAthleteList() {
        when(cooperationRepository.findByCoachIdAndStatus(COACH_ID, CooperationStatus.ACTIVE))
                .thenReturn(List.of());

        assertEquals(0, service.athletes(COACH_ID).size());

        verify(athleteAccessGuard, never()).requireActiveCooperation(anyLong(), anyLong());
    }

    @Test
    @DisplayName("brak relacji blokuje utworzenie planu i nie sięga do serwisu")
    void shouldDenyPlanCreation() {
        givenNoCooperation();

        assertThrows(AccessDeniedException.class,
                () -> service.createTrainingPlan(COACH_ID, ATHLETE_ID, REQUEST));

        verify(trainingPlanService, never()).createTrainingPlanForAthlete(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("brak relacji blokuje edycję planu i nie sięga do serwisu")
    void shouldDenyPlanUpdate() {
        givenNoCooperation();

        assertThrows(AccessDeniedException.class,
                () -> service.updateTrainingPlan(COACH_ID, ATHLETE_ID, PLAN_ID, REQUEST));

        verify(trainingPlanService, never()).updateTrainingPlanForAthlete(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("plan zapisywany jest na koncie podopiecznego, autorstwo na trenerze")
    void shouldCreatePlanWithAthleteIdAndCoachAsAuthor() {
        service.createTrainingPlan(COACH_ID, ATHLETE_ID, REQUEST);

        verify(trainingPlanService).createTrainingPlanForAthlete(ATHLETE_ID, COACH_ID, REQUEST);
    }

}
