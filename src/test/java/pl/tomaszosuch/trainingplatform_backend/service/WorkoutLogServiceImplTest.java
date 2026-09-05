package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutLogRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutLogResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.TrainingPlanNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutLogNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.WorkoutLogMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.WorkoutLogServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkoutLogService tests")
public class WorkoutLogServiceImplTest {

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkoutCategoryRepository categoryRepository;

    @Mock
    private WorkoutLogMapper workoutLogMapper;

    @Mock
    private TrainingPlanRepository trainingPlanRepository;

    @InjectMocks
    private WorkoutLogServiceImpl workoutLogService;

    private User owner;
    private WorkoutCategory category;
    private TrainingPlan plan;
    private WorkoutLog log;
    private WorkoutLogResponse logResponse;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long LOG_ID = 10L;
    private static final Long CATEGORY_ID = 5L;
    private static final Long PLAN_ID = 7L;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(OWNER_ID)
                .email("jan@example.com")
                .firstName("Jan")
                .lastName("Kowalski")
                .role(Role.USER)
                .isActive(true)
                .build();

        category = WorkoutCategory.builder()
                .id(CATEGORY_ID)
                .name("Taniec")
                .color("#9B59B6")
                .iconName("dance")
                .build();

        plan = TrainingPlan.builder()
                .id(PLAN_ID)
                .user(owner)
                .category(category)
                .title("Salsa")
                .plannedDate(LocalDate.now())
                .build();

        log = WorkoutLog.builder()
                .id(LOG_ID)
                .user(owner)
                .category(category)
                .performedDate(LocalDate.now())
                .durationMin(60)
                .intensity(7)
                .build();

        logResponse = new WorkoutLogResponse(
                LOG_ID, null, CATEGORY_ID, "Taniec", "#9B59B6", null,
                LocalDate.now(), null, 60, 7, null);
    }

    @Test
    @DisplayName("powinien zwrócić wszystkie wpisy gdy brak filtrów")
    public void shouldReturnAllLogsWhenNoFilters() {
        // given
        when(workoutLogRepository.findByUserIdOrderByPerformedDateDesc(OWNER_ID)).thenReturn(List.of(log));
        when(workoutLogMapper.toResponse(log)).thenReturn(logResponse);

        // when
        List<WorkoutLogResponse> response = workoutLogService.getUserLogs(OWNER_ID, null, null, null);

        // then
        assertEquals(1, response.size());
        assertEquals(LOG_ID, response.get(0).id());
        assertEquals(CATEGORY_ID, response.get(0).categoryId());
        assertEquals("Taniec", response.get(0).categoryName());
        assertEquals("#9B59B6", response.get(0).categoryColor());
        assertEquals(LocalDate.now(), response.get(0).performedDate());
        assertEquals(60, response.get(0).durationMin());
        assertEquals(7, response.get(0).intensity());
        verify(workoutLogRepository).findByUserIdOrderByPerformedDateDesc(OWNER_ID);
    }

    @Test
    @DisplayName("powinien filtrować po kategorii gdy podano categoryId")
    public void shouldFilterByCategoryWhenProvided() {
        // given
        when(workoutLogRepository.findByUserIdAndCategoryIdOrderByPerformedDateDesc(OWNER_ID, CATEGORY_ID))
                .thenReturn(List.of(log));
        when(workoutLogMapper.toResponse(log)).thenReturn(logResponse);

        // when
        List<WorkoutLogResponse> response = workoutLogService.getUserLogs(OWNER_ID, CATEGORY_ID, null, null);

        // then
        assertEquals(1, response.size());
        assertEquals(LOG_ID, response.get(0).id());
        assertEquals(CATEGORY_ID, response.get(0).categoryId());
        assertEquals("Taniec", response.get(0).categoryName());
        assertEquals("#9B59B6", response.get(0).categoryColor());
        assertEquals(LocalDate.now(), response.get(0).performedDate());
        assertEquals(60, response.get(0).durationMin());
        assertEquals(7, response.get(0).intensity());
        verify(workoutLogRepository).findByUserIdAndCategoryIdOrderByPerformedDateDesc(OWNER_ID, CATEGORY_ID);
    }

    @Test
    @DisplayName("powinien filtrować po zakresie dat gdy podano from i to")
    public void shouldFilterByDateRangeWhenProvided() {
        // given
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = LocalDate.now().plusDays(5);
        when(workoutLogRepository.findByUserIdAndPerformedDateBetweenOrderByPerformedDateDesc(OWNER_ID, from, to))
                .thenReturn(List.of(log));
        when(workoutLogMapper.toResponse(log)).thenReturn(logResponse);

        // when
        List<WorkoutLogResponse> response = workoutLogService.getUserLogs(OWNER_ID, null, from, to);

        // then
        assertEquals(1, response.size());
        assertEquals(LOG_ID, response.get(0).id());
        assertEquals(CATEGORY_ID, response.get(0).categoryId());
        assertEquals("Taniec", response.get(0).categoryName());
        assertEquals("#9B59B6", response.get(0).categoryColor());
        assertEquals(LocalDate.now(), response.get(0).performedDate());
        assertEquals(60, response.get(0).durationMin());
        assertEquals(7, response.get(0).intensity());
        verify(workoutLogRepository).findByUserIdAndPerformedDateBetweenOrderByPerformedDateDesc(OWNER_ID, from, to);
    }

    @Test
    @DisplayName("powinien zwrócić wpis gdy należy do użytkownika")
    public void shouldReturnLogWhenOwned() {
        // given
        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.of(log));
        when(workoutLogMapper.toResponse(log)).thenReturn(logResponse);

        // when
        WorkoutLogResponse response = workoutLogService.getById(OWNER_ID, LOG_ID);

        // then
        assertEquals(LOG_ID, response.id());
        assertEquals(CATEGORY_ID, response.categoryId());
        assertEquals("Taniec", response.categoryName());
        assertEquals("#9B59B6", response.categoryColor());
        assertEquals(LocalDate.now(), response.performedDate());
        assertEquals(60, response.durationMin());
        assertEquals(7, response.intensity());
        verify(workoutLogRepository).findById(LOG_ID);
    }

    @Test
    @DisplayName("powinien rzucić 404 gdy wpis nie istnieje")
    public void shouldThrowNotFoundWhenLogDoesNotExist() {
        // given
        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(WorkoutLogNotFoundException.class, () -> workoutLogService.getById(OWNER_ID, LOG_ID));
    }

    @Test
    @DisplayName("powinien rzucić 403 gdy wpis należy do innego użytkownika")
    public void shouldThrowAccessDeniedWhenNotOwned() {
        // given
        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.of(log));

        // when & then
        assertThrows(AccessDeniedException.class, () -> workoutLogService.getById(OTHER_USER_ID, LOG_ID));
    }

    @Test
    @DisplayName("powinien utworzyć wpis ad-hoc gdy planId jest null")
    public void shouldCreateAdHocLogWhenPlanIdIsNull() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(workoutLogRepository.save(any(WorkoutLog.class))).thenReturn(log);
        when(workoutLogMapper.toResponse(any(WorkoutLog.class))).thenReturn(logResponse);

        WorkoutLogResponse result = workoutLogService.create(OWNER_ID, request);

        assertNotNull(result);
        verify(workoutLogRepository).save(argThat(l -> l.getPlan() == null));
        verify(trainingPlanRepository, never()).findById(any());
    }

    @Test
    @DisplayName("powinien utworzyć wpis powiązany z planem gdy planId podano")
    void shouldCreateLogLinkedToPlanWhenPlanIdProvided() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, PLAN_ID, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(trainingPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(workoutLogRepository.save(any(WorkoutLog.class))).thenReturn(log);
        when(workoutLogMapper.toResponse(any(WorkoutLog.class))).thenReturn(logResponse);

        workoutLogService.create(OWNER_ID, request);

        verify(workoutLogRepository).save(argThat(l -> l.getPlan() != null && l.getPlan().getId().equals(PLAN_ID)));
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy użytkownik nie istnieje")
    void shouldThrowWhenUserNotFound() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> workoutLogService.create(OWNER_ID, request));

        verify(workoutLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy kategoria nie istnieje")
    void shouldThrowWhenCategoryNotFound() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(WorkoutCategoryNotFoundException.class,
                () -> workoutLogService.create(OWNER_ID, request));

        verify(workoutLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy podany plan nie istnieje")
    void shouldThrowWhenPlanNotFound() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, PLAN_ID, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(trainingPlanRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(TrainingPlanNotFoundException.class,
                () -> workoutLogService.create(OWNER_ID, request));

        verify(workoutLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić 403 gdy podany plan należy do innego użytkownika")
    void shouldThrowAccessDeniedWhenPlanBelongsToOtherUser() {
        User otherUser = User.builder().id(OTHER_USER_ID).build();
        TrainingPlan otherPlan = TrainingPlan.builder()
                .id(PLAN_ID).user(otherUser).build();

        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, PLAN_ID, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(trainingPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(otherPlan));

        assertThrows(AccessDeniedException.class,
                () -> workoutLogService.create(OWNER_ID, request));

        verify(workoutLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien zaktualizować wpis gdy należy do użytkownika")
    void shouldUpdateLogWhenOwned() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), null, 90, 9, "świetny trening");

        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.of(log));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(workoutLogRepository.save(any(WorkoutLog.class))).thenReturn(log);
        when(workoutLogMapper.toResponse(any(WorkoutLog.class))).thenReturn(logResponse);

        workoutLogService.update(OWNER_ID, LOG_ID, request);

        verify(workoutLogRepository).save(argThat(l -> l.getDurationMin() == 90 && l.getIntensity() == 9));
    }

    @Test
    @DisplayName("powinien usunąć wpis gdy należy do użytkownika")
    void shouldDeleteLogWhenOwned() {
        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.of(log));

        workoutLogService.delete(OWNER_ID, LOG_ID);

        verify(workoutLogRepository).delete(log);
    }

    @Test
    @DisplayName("powinien rzucić 403 gdy edytowany wpis należy do innego użytkownika")
    void shouldThrowAccessDeniedWhenUpdatingNotOwnedLog() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), null, 90, 9, "próba edycji cudzego wpisu");

        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.of(log));

        assertThrows(AccessDeniedException.class,
                () -> workoutLogService.update(OTHER_USER_ID, LOG_ID, request));

        verify(workoutLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić 403 gdy usuwany wpis należy do innego użytkownika")
    void shouldThrowAccessDeniedWhenDeletingNotOwnedLog() {
        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.of(log));

        assertThrows(AccessDeniedException.class,
                () -> workoutLogService.delete(OTHER_USER_ID, LOG_ID));

        verify(workoutLogRepository, never()).delete(any(WorkoutLog.class));
    }

    @Test
    @DisplayName("powinien rzucić 404 gdy edytowany wpis nie istnieje")
    void shouldThrowNotFoundWhenUpdatingNonExistingLog() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), null, 60, 5, null);

        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.empty());

        assertThrows(WorkoutLogNotFoundException.class,
                () -> workoutLogService.update(OWNER_ID, LOG_ID, request));

        verify(workoutLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić 404 gdy usuwany wpis nie istnieje")
    void shouldThrowNotFoundWhenDeletingNonExistingLog() {
        when(workoutLogRepository.findById(LOG_ID)).thenReturn(Optional.empty());

        assertThrows(WorkoutLogNotFoundException.class,
                () -> workoutLogService.delete(OWNER_ID, LOG_ID));

        verify(workoutLogRepository, never()).delete(any(WorkoutLog.class));
    }

    @Test
    @DisplayName("powinien zapisać godzinę rozpoczęcia gdy została podana")
    void shouldPersistPerformedTimeWhenProvided() {
        LocalTime performedTime = LocalTime.of(18, 30);

        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), performedTime, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(workoutLogRepository.save(any(WorkoutLog.class))).thenReturn(log);
        when(workoutLogMapper.toResponse(any(WorkoutLog.class))).thenReturn(logResponse);

        workoutLogService.create(OWNER_ID, request);

        verify(workoutLogRepository).save(argThat(l -> performedTime.equals(l.getPerformedTime())));
    }

    @Test
    @DisplayName("powinien zapisać wpis bez godziny gdy nie została podana")
    void shouldPersistLogWithoutPerformedTime() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(workoutLogRepository.save(any(WorkoutLog.class))).thenReturn(log);
        when(workoutLogMapper.toResponse(any(WorkoutLog.class))).thenReturn(logResponse);

        workoutLogService.create(OWNER_ID, request);

        verify(workoutLogRepository).save(argThat(l -> l.getPerformedTime() == null));
    }

    @Test
    @DisplayName("powinien zapisać tytuł wpisu gdy został podany")
    void shouldPersistTitleWhenProvided() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                "Salsa — zajęcia grupowe", CATEGORY_ID, null, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(workoutLogRepository.save(any(WorkoutLog.class))).thenReturn(log);
        when(workoutLogMapper.toResponse(any(WorkoutLog.class))).thenReturn(logResponse);

        workoutLogService.create(OWNER_ID, request);

        verify(workoutLogRepository).save(argThat(l -> "Salsa — zajęcia grupowe".equals(l.getTitle())));
    }

    @Test
    @DisplayName("powinien zapisać wpis bez tytułu gdy nie został podany")
    void shouldPersistLogWithoutTitle() {
        WorkoutLogRequest request = new WorkoutLogRequest(
                null, CATEGORY_ID, null, LocalDate.now(), null, 60, 7, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(workoutLogRepository.save(any(WorkoutLog.class))).thenReturn(log);
        when(workoutLogMapper.toResponse(any(WorkoutLog.class))).thenReturn(logResponse);

        workoutLogService.create(OWNER_ID, request);

        verify(workoutLogRepository).save(argThat(l -> l.getTitle() == null));
    }
}
