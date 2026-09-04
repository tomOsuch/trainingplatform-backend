package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalStatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.GoalNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.GoalMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.GoalRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.GoalServiceImpl;
import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalServiceImplTest")
class GoalServiceImplTest {

    @Mock
    private GoalRepository goalRepository;
    @Mock
    private GoalMapper goalMapper;
    @Mock
    private GoalProgressService goalProgressService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkoutCategoryRepository workoutCategoryRepository;

    @InjectMocks
    private GoalServiceImpl service;

    private User owner;
    private User stranger;
    private WorkoutCategory category;
    private Goal goal;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("jan@example.com").role(Role.USER).build();
        stranger = User.builder().id(2L).email("obcy@example.com").role(Role.USER).build();
        category = WorkoutCategory.builder().id(5L).name("Taniec").build();
        goal = Goal.builder()
                .id(10L).user(owner).category(category)
                .title("100 godzin tańca").metric(GoalMetric.MINUTES).targetValue(6000)
                .startDate(LocalDate.of(2026, 1, 1))
                .build();
    }

    private static GoalRequest request(Long categoryId, LocalDate start, LocalDate end) {
        return new GoalRequest("100 godzin tańca", null, categoryId, GoalMetric.MINUTES, 6000, start, end);
    }

    @Test
    @DisplayName("filtr active/achieved/brak wybiera właściwe zapytanie")
    void shouldPickRepositoryQueryByStatus() {
        when(goalProgressService.progressOf(eq(1L), any())).thenReturn(Map.of());

        service.getGoals(1L, GoalStatus.ACTIVE);
        service.getGoals(1L, GoalStatus.ACHIEVED);
        service.getGoals(1L, null);

        verify(goalRepository).findByUserIdAndAchievedAtIsNullOrderByCreatedAtDesc(1L);
        verify(goalRepository).findByUserIdAndAchievedAtIsNotNullOrderByAchievedAtDesc(1L);
        verify(goalRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("lista liczy postęp jednym wywołaniem i mapuje go do każdego celu")
    void shouldMergeProgressIntoList() {
        GoalProgress progress = new GoalProgress(1500, 6000);
        when(goalRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(goal));
        when(goalProgressService.progressOf(1L, List.of(goal))).thenReturn(Map.of(10L, progress));

        service.getGoals(1L, null);

        verify(goalMapper).toResponse(goal, progress);
        verify(goalProgressService, never()).progressOf(any(Goal.class));
    }

    @Test
    @DisplayName("tworzenie bez daty początkowej ustawia dziś, bez kategorii zostawia null")
    void shouldDefaultStartDateToTodayAndAllowNoCategory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(goalProgressService.progressOf(any(Goal.class))).thenReturn(new GoalProgress(0, 6000));

        service.createGoal(1L, request(null, null, null));

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertEquals(LocalDate.now(), captor.getValue().getStartDate());
        assertNull(captor.getValue().getCategory());
        verify(workoutCategoryRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("data końcowa przed początkową zwraca błąd walidacji")
    void shouldRejectEndDateBeforeStartDate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createGoal(1L, request(null, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 5))));

        assertTrue(ex.getMessage().contains("końcowa"));
        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("termin w przeszłości nie jest błędem (US-016)")
    void shouldAcceptPastDeadline() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(goalProgressService.progressOf(any(Goal.class))).thenReturn(new GoalProgress(0, 6000));

        service.createGoal(1L, request(null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)));

        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    @DisplayName("nieistniejąca kategoria zwraca 404")
    void shouldRejectUnknownCategory() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(workoutCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WorkoutCategoryNotFoundException.class,
                () -> service.createGoal(1L, request(99L, null, null)));
    }

    @Test
    @DisplayName("edycja cudzego celu kończy się odmową")
    void shouldDenyUpdateOfForeignGoal() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));

        assertThrows(AccessDeniedException.class,
                () -> service.updateGoal(stranger.getId(), 10L, request(5L, null, null)));
        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("usunięcie cudzego celu kończy się odmową")
    void shouldDenyDeleteOfForeignGoal() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));

        assertThrows(AccessDeniedException.class, () -> service.deleteGoal(stranger.getId(), 10L));
        verify(goalRepository, never()).delete(any());
    }

    @Test
    @DisplayName("nieistniejący cel zwraca 404")
    void shouldThrowWhenGoalMissing() {
        when(goalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(GoalNotFoundException.class, () -> service.deleteGoal(1L, 99L));
    }

    @Test
    @DisplayName("edycja bez daty początkowej zachowuje dotychczasową, nie przesuwa na dziś")
    void shouldKeepStartDateOnUpdateWhenNotProvided() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(workoutCategoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(goalProgressService.progressOf(any(Goal.class))).thenReturn(new GoalProgress(0, 6000));

        service.updateGoal(1L, 10L, request(5L, null, LocalDate.of(2026, 12, 31)));

        assertEquals(LocalDate.of(2026, 1, 1), goal.getStartDate());
        assertEquals(LocalDate.of(2026, 12, 31), goal.getEndDate());
    }

    @Test
    @DisplayName("osiągnięty cel nie podlega edycji")
    void shouldRejectUpdateOfAchievedGoal() {
        goal.setAchievedAt(LocalDateTime.now());
        goal.setAchievedValue(6000);
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateGoal(1L, 10L, request(5L, null, null)));

        assertTrue(ex.getMessage().contains("Osiągnięty"));
        verify(goalRepository, never()).save(any());
    }

    @Test
    @DisplayName("usunięcie własnego celu deleguje do repozytorium")
    void shouldDeleteOwnGoal() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));

        service.deleteGoal(1L, 10L);

        verify(goalRepository).delete(goal);
    }

    @Test
    @DisplayName("oznaczenie jako osiągnięty zapisuje migawkę z aktualnego postępu")
    void shouldFreezeProgressWhenMarkedAchieved() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(goalProgressService.progressOf(goal)).thenReturn(new GoalProgress(4321, 6000));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, 10L, new GoalStatusUpdateRequest(GoalStatus.ACHIEVED));

        assertEquals(4321, goal.getAchievedValue());
        assertNotNull(goal.getAchievedAt());
    }

    @Test
    @DisplayName("ponowne oznaczenie osiągniętego celu nie nadpisuje migawki")
    void shouldNotOverwriteSnapshotWhenAlreadyAchieved() {
        goal.setAchievedAt(LocalDateTime.of(2026, 6, 1, 12, 0));
        goal.setAchievedValue(6000);
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(goalProgressService.progressOf(goal)).thenReturn(new GoalProgress(6000, 6000));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, 10L, new GoalStatusUpdateRequest(GoalStatus.ACHIEVED));

        assertEquals(6000, goal.getAchievedValue());
        assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), goal.getAchievedAt());
    }

    @Test
    @DisplayName("cofnięcie czyści migawkę i datę osiągnięcia")
    void shouldClearSnapshotWhenReverted() {
        goal.setAchievedAt(LocalDateTime.now());
        goal.setAchievedValue(6000);
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));
        when(goalProgressService.progressOf(goal)).thenReturn(new GoalProgress(6100, 6000));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, 10L, new GoalStatusUpdateRequest(GoalStatus.ACTIVE));

        assertNull(goal.getAchievedAt());
        assertNull(goal.getAchievedValue());
    }

    @Test
    @DisplayName("zmiana statusu cudzego celu kończy się odmową")
    void shouldDenyStatusChangeOfForeignGoal() {
        when(goalRepository.findById(10L)).thenReturn(Optional.of(goal));

        assertThrows(AccessDeniedException.class,
                () -> service.changeStatus(stranger.getId(), 10L, new GoalStatusUpdateRequest(GoalStatus.ACHIEVED)));
        verify(goalRepository, never()).save(any());
    }

}