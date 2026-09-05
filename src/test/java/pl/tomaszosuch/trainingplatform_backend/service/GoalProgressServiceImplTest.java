package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;
import pl.tomaszosuch.trainingplatform_backend.repository.GoalProgressView;
import pl.tomaszosuch.trainingplatform_backend.repository.GoalRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.GoalProgressServiceImpl;
import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalProgressServiceImplTest")
class GoalProgressServiceImplTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    @InjectMocks
    private GoalProgressServiceImpl service;

    private static Goal goal(Long id, GoalMetric metric, int target) {
        return Goal.builder()
                .id(id)
                .user(User.builder().id(7L).build())
                .metric(metric)
                .targetValue(target)
                .startDate(LocalDate.of(2026, 1, 1))
                .build();
    }

    private static WorkoutLog log(Integer minutes) {
        return WorkoutLog.builder().durationMin(minutes).performedDate(LocalDate.of(2026, 2, 1)).build();
    }

    private static GoalProgressView row(Long goalId, long sessions, long minutes) {
        return new GoalProgressView() {
            public Long getGoalId() { return goalId; }
            public Long getSessions() { return sessions; }
            public Long getMinutes() { return minutes; }
        };
    }

    @SuppressWarnings("unchecked")
    private void givenMatchingLogs(List<WorkoutLog> logs) {
        when(workoutLogRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(logs);
    }

    @SuppressWarnings("unchecked")
    private void verifyNoLogQuery() {
        verify(workoutLogRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @DisplayName("lista: cel SESSIONS bierze liczbę wpisów, cel MINUTES sumę minut")
    void shouldPickValueByMetric() {
        Goal sessions = goal(1L, GoalMetric.SESSIONS, 10);
        Goal minutes = goal(2L, GoalMetric.MINUTES, 600);
        when(goalRepository.findActiveProgressByUserId(7L))
                .thenReturn(List.of(row(1L, 4, 300), row(2L, 4, 300)));

        Map<Long, GoalProgress> result = service.progressOf(7L, List.of(sessions, minutes));

        assertEquals(4, result.get(1L).currentValue());
        assertEquals(300, result.get(2L).currentValue());
    }

    @Test
    @DisplayName("pojedynczy cel: postęp liczony z listy wliczonych wpisów")
    void shouldAggregateMatchingLogsForSingleGoal() {
        givenMatchingLogs(List.of(log(60), log(null), log(30)));

        GoalProgress sessions = service.progressOf(goal(1L, GoalMetric.SESSIONS, 10));
        GoalProgress minutes = service.progressOf(goal(2L, GoalMetric.MINUTES, 600));

        assertEquals(3, sessions.currentValue());   // wpis bez czasu trwania to nadal sesja
        assertEquals(90, minutes.currentValue());   // ...ale nie dodaje minut
    }

    @Test
    @DisplayName("postęp z podanej listy nie odpytuje repozytorium")
    void shouldComputeFromProvidedListWithoutQuery() {
        GoalProgress progress = service.progressOf(goal(1L, GoalMetric.MINUTES, 100), List.of(log(40), log(20)));

        assertEquals(60, progress.currentValue());
        verifyNoLogQuery();
    }

    @Test
    @DisplayName("cel osiągnięty zwraca migawkę i nie odpytuje bazy")
    void shouldReturnSnapshotForAchievedGoal() {
        Goal achieved = goal(1L, GoalMetric.MINUTES, 600);
        achieved.setAchievedAt(LocalDateTime.of(2026, 6, 1, 12, 0));
        achieved.setAchievedValue(612);

        GoalProgress progress = service.progressOf(achieved);

        assertEquals(612, progress.currentValue());
        assertTrue(progress.targetReached());
        verifyNoLogQuery();
    }

    @Test
    @DisplayName("cel osiągnięty zwraca migawkę także przy podanej liście")
    void shouldReturnSnapshotForAchievedGoalEvenWithList() {
        Goal achieved = goal(1L, GoalMetric.MINUTES, 600);
        achieved.setAchievedAt(LocalDateTime.now());
        achieved.setAchievedValue(600);

        GoalProgress progress = service.progressOf(achieved, List.of(log(1000), log(1000)));

        assertEquals(600, progress.currentValue());
    }

    @Test
    @DisplayName("lista złożona wyłącznie z celów osiągniętych nie wykonuje zapytania")
    void shouldSkipQueryWhenAllGoalsAchieved() {
        Goal achieved = goal(1L, GoalMetric.SESSIONS, 10);
        achieved.setAchievedAt(LocalDateTime.now());
        achieved.setAchievedValue(10);

        Map<Long, GoalProgress> result = service.progressOf(7L, List.of(achieved));

        assertEquals(10, result.get(1L).currentValue());
        verify(goalRepository, never()).findActiveProgressByUserId(anyLong());
    }

    @Test
    @DisplayName("N celów to jedno zapytanie")
    void shouldRunSingleQueryForManyGoals() {
        List<Goal> goals = List.of(
                goal(1L, GoalMetric.SESSIONS, 5),
                goal(2L, GoalMetric.SESSIONS, 5),
                goal(3L, GoalMetric.MINUTES, 500));
        when(goalRepository.findActiveProgressByUserId(7L))
                .thenReturn(List.of(row(1L, 1, 60), row(2L, 2, 120), row(3L, 3, 180)));

        service.progressOf(7L, goals);

        verify(goalRepository, times(1)).findActiveProgressByUserId(7L);
        verifyNoLogQuery();
    }

    @Test
    @DisplayName("cel bez wpisów ma postęp zero")
    void shouldDefaultToZeroWhenNoLogs() {
        givenMatchingLogs(List.of());

        GoalProgress progress = service.progressOf(goal(1L, GoalMetric.SESSIONS, 5));

        assertEquals(0, progress.currentValue());
        assertEquals(0, progress.percent());
        assertFalse(progress.targetReached());
    }

    @Test
    @DisplayName("procent jest obcięty do 100")
    void shouldCapPercentAtHundred() {
        assertEquals(100, new GoalProgress(150, 100).percent());
        assertEquals(50, new GoalProgress(50, 100).percent());
    }

}