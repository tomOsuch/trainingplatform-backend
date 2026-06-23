package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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

import pl.tomaszosuch.trainingplatform_backend.dto.request.StatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.TrainingPlanRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.TrainingPlanNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.TrainingPlanMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.TrainingPlanServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingPlanService")
public class TrainingPlanServiceImplTest {

    @Mock
    private TrainingPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkoutCategoryRepository categoryRepository;

    @Mock
    private TrainingPlanMapper planMapper;

    @InjectMocks
    private TrainingPlanServiceImpl planService;

    private User owner;
    private WorkoutCategory category;
    private TrainingPlan plan;
    private TrainingPlanResponse planResponse;

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PLAN_ID = 10L;
    private static final Long CATEGORY_ID = 5L;

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
                .title("Salsa wieczorna")
                .plannedDate(LocalDate.now().plusDays(3))
                .durationMin(60)
                .status(PlanStatus.PLANNED)
                .build();

        planResponse = new TrainingPlanResponse(
                PLAN_ID, "Salsa wieczorna", CATEGORY_ID, "Taniec", "#9B59B6",
                LocalDate.now().plusDays(3), null, 60, null, PlanStatus.PLANNED);
    }

    @Test
    @DisplayName("powinien zwrócić plany użytkownika gdy brak zakresu dat")
    public void shouldReturnAllUserPlansWhenNoDateRange() {
        // given
        when(planRepository.findByUserIdOrderByPlannedDateAsc(OWNER_ID)).thenReturn(List.of(plan));
        when(planMapper.toResponse(plan)).thenReturn(planResponse);

        // when
        List<TrainingPlanResponse> response = planService.getTrainingPlansByUserId(OWNER_ID, null, null);

        // then
        assert response != null;
        assert response.size() == 1;
        assert response.get(0).equals(planResponse);
    }

    @Test
    @DisplayName("powinien filtrować po zakresie dat gdy podano from i to")
    public void shouldFilterByDateRangeWhenProvided() {
        // given
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = LocalDate.now().plusDays(5);
        when(planRepository.findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(OWNER_ID, from, to))
                .thenReturn(List.of(plan));
        when(planMapper.toResponse(plan)).thenReturn(planResponse);

        // when
        List<TrainingPlanResponse> response = planService.getTrainingPlansByUserId(OWNER_ID, from, to);

        // then
        assert response != null;
        assert response.size() == 1;
        assert response.get(0).equals(planResponse);
    }

    @Test
    @DisplayName("powinien zwrócić plan gdy należy do użytkownika")
    public void shouldThrowNotFoundWhenPlanDoesNotExist() {
        // given
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        // when & then
        assertThrows(TrainingPlanNotFoundException.class,
                () -> planService.getTrainingPlanById(PLAN_ID, OTHER_USER_ID));
    }

    @Test
    @DisplayName("powinien rzucić 403 gdy plan należy do innego użytkownika")
    public void shouldThrowAccessDeniedWhenPlanBelongsToOtherUser() {
        // given
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        // when & then
        assertThrows(AccessDeniedException.class, () -> planService.getTrainingPlanById(PLAN_ID, OTHER_USER_ID));
    }

    @Test
    @DisplayName("powinien rzucić UserNotFoundException gdy użytkownik nie istnieje")
    public void shouldThrowWhenUserNotFound() {
        TrainingPlanRequest request = new TrainingPlanRequest(
                "Salsa", CATEGORY_ID, LocalDate.now().plusDays(1), null, 60, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> planService.createTrainingPlan(OWNER_ID, request));

        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy kategoria nie istnieje")
    void shouldThrowWhenCategoryNotFound() {
        TrainingPlanRequest request = new TrainingPlanRequest(
                "Salsa", CATEGORY_ID, LocalDate.now().plusDays(1), null, 60, null);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(WorkoutCategoryNotFoundException.class,
                () -> planService.createTrainingPlan(OWNER_ID, request));

        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien zaktualizować plan gdy należy do użytkownika")
    void shouldUpdatePlanWhenOwned() {
        TrainingPlanRequest request = new TrainingPlanRequest(
                "Salsa poranna", CATEGORY_ID,
                LocalDate.now().plusDays(5), null, 90, "nowe notatki");

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(planRepository.save(any(TrainingPlan.class))).thenReturn(plan);
        when(planMapper.toResponse(any(TrainingPlan.class))).thenReturn(planResponse);

        planService.updateTrainingPlan(OWNER_ID, PLAN_ID, request);

        verify(planRepository).save(argThat(p -> p.getTitle().equals("Salsa poranna") &&
                p.getDurationMin() == 90));
    }

    @Test
    @DisplayName("powinien rzucić 403 gdy plan należy do innego użytkownika")
    void shouldThrowAccessDeniedWhenNotOwned() {
        TrainingPlanRequest request = new TrainingPlanRequest(
                "Salsa", CATEGORY_ID, LocalDate.now().plusDays(1), null, 60, null);

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        assertThrows(AccessDeniedException.class,
                () -> planService.updateTrainingPlan(OTHER_USER_ID, PLAN_ID, request));

        verify(planRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien zmienić status planu")
    void shouldChangeStatus() {
        StatusUpdateRequest request = new StatusUpdateRequest(PlanStatus.COMPLETED);

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.save(any(TrainingPlan.class))).thenReturn(plan);
        when(planMapper.toResponse(any(TrainingPlan.class))).thenReturn(planResponse);

        planService.changeStatus(OWNER_ID, PLAN_ID, request);

        verify(planRepository).save(argThat(p -> p.getStatus() == PlanStatus.COMPLETED));
    }

    @Test
    @DisplayName("powinien usunąć plan gdy należy do użytkownika")
    void shouldDeletePlanWhenOwned() {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.deleteTrainingPlan(OWNER_ID, PLAN_ID);

        verify(planRepository).delete(plan);
    }

}
