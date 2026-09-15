package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutTemplateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutTemplateResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutTemplate;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutTemplateNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.WorkoutTemplateMapper;
import pl.tomaszosuch.trainingplatform_backend.mapper.WorkoutTemplateMapperImpl;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutTemplateRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.WorkoutTemplateServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkoutTemplateServiceImplTest")
class WorkoutTemplateServiceImplTest {

    private static final Long OWNER_ID = 3L;
    private static final Long OTHER_ID = 9L;
    private static final Long TEMPLATE_ID = 10L;
    private static final Long CATEGORY_ID = 5L;

    @Mock
    private WorkoutTemplateRepository workoutTemplateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkoutCategoryRepository workoutCategoryRepository;

    // Prawdziwy mapper: test ma pokazać, co dostanie klient, a nie co zwróci atrapa.
    @Spy
    private WorkoutTemplateMapper workoutTemplateMapper = new WorkoutTemplateMapperImpl();

    @InjectMocks
    private WorkoutTemplateServiceImpl workoutTemplateService;

    private User owner;
    private WorkoutCategory dance;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(OWNER_ID).email("wlasciciel@example.com").password("hash")
                .firstName("Jan").lastName("Testowy").role(Role.USER).isActive(true)
                .build();

        dance = WorkoutCategory.builder()
                .id(CATEGORY_ID).name("Taniec").color("#9B59B6").iconName("music")
                .build();
    }

    private WorkoutTemplate template(User user) {
        return WorkoutTemplate.builder()
                .id(TEMPLATE_ID).user(user).category(dance)
                .name("Wtorkowy trening").description("Rozgrzewka i figury").durationMin(60)
                .build();
    }

    private static WorkoutTemplateRequest request() {
        return new WorkoutTemplateRequest("Wtorkowy trening", "Rozgrzewka i figury", CATEGORY_ID, 60);
    }

    private User otherUser() {
        return User.builder().id(OTHER_ID).email("obcy@example.com").password("hash")
                .firstName("Ewa").lastName("Obca").role(Role.USER).isActive(true)
                .build();
    }

    @Test
    @DisplayName("lista pyta repozytorium o szablony zalogowanego użytkownika")
    void shouldListOwnTemplates() {
        when(workoutTemplateRepository.findByUserIdOrderByNameAsc(OWNER_ID))
                .thenReturn(List.of(template(owner)));

        List<WorkoutTemplateResponse> templates = workoutTemplateService.getTemplates(OWNER_ID);

        assertEquals(1, templates.size());
        assertEquals("Wtorkowy trening", templates.get(0).name());
        assertEquals("Taniec", templates.get(0).categoryName());
        assertEquals("#9B59B6", templates.get(0).categoryColor());
        assertEquals("music", templates.get(0).categoryIconName());
    }

    @Test
    @DisplayName("tworzenie przypisuje szablon właścicielowi i kategorii z żądania")
    void shouldCreateTemplateForOwner() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(workoutCategoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(dance));
        when(workoutTemplateRepository.save(any(WorkoutTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        workoutTemplateService.createTemplate(OWNER_ID, request());

        ArgumentCaptor<WorkoutTemplate> captor = ArgumentCaptor.forClass(WorkoutTemplate.class);
        verify(workoutTemplateRepository).save(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getUser().getId());
        assertEquals(CATEGORY_ID, captor.getValue().getCategory().getId());
        assertEquals(60, captor.getValue().getDurationMin());
    }

    @Test
    @DisplayName("tworzenie z nieistniejącą kategorią kończy się 404 i niczego nie zapisuje")
    void shouldRejectUnknownCategory() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(workoutCategoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(WorkoutCategoryNotFoundException.class,
                () -> workoutTemplateService.createTemplate(OWNER_ID, request()));

        verify(workoutTemplateRepository, never()).save(any(WorkoutTemplate.class));
    }

    @Test
    @DisplayName("edycja własnego szablonu podmienia wszystkie pola")
    void shouldUpdateOwnTemplate() {
        WorkoutCategory gym = WorkoutCategory.builder()
                .id(6L).name("Siłownia").color("#E67E22").iconName("dumbbell").build();
        WorkoutTemplate existing = template(owner);

        when(workoutTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(existing));
        when(workoutCategoryRepository.findById(6L)).thenReturn(Optional.of(gym));
        when(workoutTemplateRepository.save(any(WorkoutTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        WorkoutTemplateResponse response = workoutTemplateService.updateTemplate(
                OWNER_ID, TEMPLATE_ID, new WorkoutTemplateRequest("Poranna siła", null, 6L, 45));

        assertEquals("Poranna siła", response.name());
        assertEquals(6L, response.categoryId());
        assertEquals(45, response.durationMin());
        assertEquals(null, response.description());
    }

    @Test
    @DisplayName("odczyt cudzego szablonu kończy się odmową")
    void shouldRejectReadingForeignTemplate() {
        when(workoutTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template(otherUser())));

        assertThrows(AccessDeniedException.class,
                () -> workoutTemplateService.getTemplate(OWNER_ID, TEMPLATE_ID));
    }

    @Test
    @DisplayName("edycja cudzego szablonu kończy się odmową i niczego nie zapisuje")
    void shouldRejectUpdatingForeignTemplate() {
        when(workoutTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template(otherUser())));

        assertThrows(AccessDeniedException.class,
                () -> workoutTemplateService.updateTemplate(OWNER_ID, TEMPLATE_ID, request()));

        verify(workoutTemplateRepository, never()).save(any(WorkoutTemplate.class));
    }

    @Test
    @DisplayName("usunięcie cudzego szablonu kończy się odmową i niczego nie kasuje")
    void shouldRejectDeletingForeignTemplate() {
        when(workoutTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template(otherUser())));

        assertThrows(AccessDeniedException.class,
                () -> workoutTemplateService.deleteTemplate(OWNER_ID, TEMPLATE_ID));

        verify(workoutTemplateRepository, never()).delete(any(WorkoutTemplate.class));
    }

    @Test
    @DisplayName("usunięcie własnego szablonu przechodzi bez sprawdzania użycia")
    void shouldDeleteOwnTemplate() {
        WorkoutTemplate own = template(owner);
        when(workoutTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(own));

        workoutTemplateService.deleteTemplate(OWNER_ID, TEMPLATE_ID);

        verify(workoutTemplateRepository).delete(own);
    }

    @Test
    @DisplayName("nieistniejący szablon daje 404, nie odmowę")
    void shouldThrowWhenTemplateNotFound() {
        when(workoutTemplateRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WorkoutTemplateNotFoundException.class,
                () -> workoutTemplateService.getTemplate(OWNER_ID, 99L));
    }
}