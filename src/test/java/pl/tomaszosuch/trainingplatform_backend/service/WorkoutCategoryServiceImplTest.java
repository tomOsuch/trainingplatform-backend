package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutCategoryRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutCategoryResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.WorkoutCategoryMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.WorkoutCategoryServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkoutCategoryServiceImplTest")
public class WorkoutCategoryServiceImplTest {

    @Mock
    private WorkoutCategoryRepository workoutCategoryRepository;

    @InjectMocks
    private WorkoutCategoryServiceImpl workoutCategoryService;

    @Mock
    private WorkoutCategoryMapper workoutCategoryMapper;

    @Mock
    private TrainingPlanRepository trainingPlanRepository;

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    private WorkoutCategory category;
    private WorkoutCategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        category = WorkoutCategory.builder()
                .id(1L)
                .name("Taniec")
                .color("#9B59B6")
                .iconName("dance")
                .build();

        categoryResponse = new WorkoutCategoryResponse(
                1L, "Taniec", "#9B59B6", "dance");
    }

    @Test
    @DisplayName("powinien zwrócić listę wszystkich kategorii")
    public void shouldReturnAllCategories() {
        // given
        when(workoutCategoryRepository.findAll()).thenReturn(List.of(category));
        when(workoutCategoryMapper.toResponse(category)).thenReturn(categoryResponse);

        // when
        List<WorkoutCategoryResponse> response = workoutCategoryService.getAllCategories();

        // then
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).id());
        assertEquals("Taniec", response.get(0).name());
        assertEquals("#9B59B6", response.get(0).color());
        assertEquals("dance", response.get(0).iconName());

    }

    @Test
    @DisplayName("powinien zwrócić pustą listę gdy brak kategorii")
    public void shouldReturnEmptyListWhenNoCategories() {
        // given
        when(workoutCategoryRepository.findAll()).thenReturn(List.of());

        // when
        List<WorkoutCategoryResponse> response = workoutCategoryService.getAllCategories();

        // then
        assertNotNull(response);
        assertEquals(0, response.size());
    }

    @Test
    @DisplayName("powinien zwrócić kategorię gdy istnieje")
    public void shouldReturnCategoryWhenExists() {
        // given
        when(workoutCategoryRepository.findById(1L)).thenReturn(java.util.Optional.of(category));
        when(workoutCategoryMapper.toResponse(category)).thenReturn(categoryResponse);

        // when
        WorkoutCategoryResponse response = workoutCategoryService.getCategoryById(1L);

        // then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Taniec", response.name());
        assertEquals("#9B59B6", response.color());
        assertEquals("dance", response.iconName());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy kategoria nie istnieje")
    public void shouldThrowExceptionWhenCategoryDoesNotExist() {
        // given
        when(workoutCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(WorkoutCategoryNotFoundException.class, () -> workoutCategoryService.getCategoryById(99L));
    }

    @Test
    @DisplayName("powinien utworzyć kategorię gdy nazwa jest unikalna")
    public void houldCreateCategoryWhenNameIsUnique() {
        // given
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Joga", "#3498DB", "yoga");

        when(workoutCategoryRepository.existsByName("Joga")).thenReturn(false);
        when(workoutCategoryRepository.save(any(WorkoutCategory.class)))
                .thenReturn(category);
        when(workoutCategoryMapper.toResponse(any(WorkoutCategory.class)))
                .thenReturn(categoryResponse);

        // when
        WorkoutCategoryResponse response = workoutCategoryService.createCategory(request);

        // then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Taniec", response.name());
        assertEquals("#9B59B6", response.color());
        assertEquals("dance", response.iconName());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy nazwa już istnieje")
    public void shouldThrowExceptionWhenNameAlreadyExists() {
        // given
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Taniec", "#9B59B6", "dance");

        when(workoutCategoryRepository.existsByName("Taniec")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> workoutCategoryService.createCategory(request));

        assertTrue(ex.getMessage().contains("już istnieje"));
        verify(workoutCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien zaktualizować kategorię gdy istnieje")
    public void shouldUpdateCategoryWhenExists() {
        // given
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Joga", "#3498DB", "yoga");

        when(workoutCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(workoutCategoryRepository.existsByName("Joga"))
                .thenReturn(false);
        when(workoutCategoryRepository.save(any(WorkoutCategory.class)))
                .thenReturn(category);
        when(workoutCategoryMapper.toResponse(any(WorkoutCategory.class)))
                .thenReturn(categoryResponse);

        // when
        WorkoutCategoryResponse result = workoutCategoryService.updateCategory(1L, request);

        // then
        assertNotNull(result);
        verify(workoutCategoryRepository).save(any(WorkoutCategory.class));
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy kategoria nie istnieje")
    public void shouldThrowExceptionWhenNotFound() {
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Joga", "#3498DB", "yoga");

        when(workoutCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WorkoutCategoryNotFoundException.class,
                () -> workoutCategoryService.updateCategory(99L, request));

        verify(workoutCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy nowa nazwa jest zajęta przez inną kategorię")
    public void shouldThrowExceptionWhenNewNameIsTaken() {
        // given
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Joga", "#3498DB", "yoga");

        when(workoutCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(workoutCategoryRepository.existsByName("Joga")).thenReturn(true);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> workoutCategoryService.updateCategory(1L, request));

        verify(workoutCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy kategoria nie istnieje")
    public void shouldThrowExceptionWhenCategoryDoesNotExistToDelete() {
        when(workoutCategoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WorkoutCategoryNotFoundException.class,
                () -> workoutCategoryService.deleteCategory(99L));

        verify(workoutCategoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("powinien usunąć kategorię gdy nie jest używana")
    void shouldDeleteCategoryWhenNotInUse() {
        // given
        when(workoutCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(trainingPlanRepository.existsByCategoryId(1L)).thenReturn(false);
        when(workoutLogRepository.existsByCategoryId(1L)).thenReturn(false);

        // when
        workoutCategoryService.deleteCategory(1L);

        // then
        verify(workoutCategoryRepository).delete(category);
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy kategoria jest używana przez plan treningowy")
    void shouldThrowExceptionWhenUsedByTrainingPlan() {
        // given
        when(workoutCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(trainingPlanRepository.existsByCategoryId(1L)).thenReturn(true);

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> workoutCategoryService.deleteCategory(1L));

        assertTrue(ex.getMessage().contains("używana"));
        verify(workoutCategoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy kategoria jest używana przez wpis dziennika")
    void shouldThrowExceptionWhenUsedByWorkoutLog() {
        // given
        when(workoutCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(trainingPlanRepository.existsByCategoryId(1L)).thenReturn(false);
        when(workoutLogRepository.existsByCategoryId(1L)).thenReturn(true);

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> workoutCategoryService.deleteCategory(1L));

        assertTrue(ex.getMessage().contains("używana"));
        verify(workoutCategoryRepository, never()).delete(any());
    }

}
