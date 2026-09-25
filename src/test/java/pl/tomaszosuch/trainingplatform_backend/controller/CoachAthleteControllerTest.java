package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.http.MediaType;

import pl.tomaszosuch.trainingplatform_backend.dto.request.TrainingPlanRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.AthleteResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;
import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;
import pl.tomaszosuch.trainingplatform_backend.exception.PlanAuthorshipException;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.CoachAthleteService;

@WebMvcTest(controllers = CoachAthleteController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(CoachAthleteControllerTest.TestConfig.class)
@DisplayName("CoachAthleteControllerTest")
class CoachAthleteControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    private static final String GUARD_MESSAGE = "Nie prowadzisz tej osoby";
    private static final String RESPONSE_MESSAGE = "Brak uprawnień";
    private static final Long ATHLETE_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoachAthleteService coachAthleteService;

    @Autowired
    private ObjectMapper objectMapper;

    private User currentUser;
    private TrainingPlanRequest validRequest;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L).email("trener@example.com").firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(true)
                .build();

        validRequest = new TrainingPlanRequest(
                "Interwały", 5L, LocalDate.now().plusDays(2), null, 45, null);
    }

    @Test
    @DisplayName("plany bez relacji: 403")
    void shouldReturn403ForTrainingPlans() throws Exception {
        when(coachAthleteService.trainingPlans(anyLong(), anyLong(), any(), any()))
                .thenThrow(new AccessDeniedException(GUARD_MESSAGE));

        mockMvc.perform(get("/coach/athletes/2/training-plans").with(user(currentUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RESPONSE_MESSAGE));
    }

    @Test
    @DisplayName("dziennik bez relacji: 403")
    void shouldReturn403ForWorkoutLogs() throws Exception {
        when(coachAthleteService.workoutLogs(anyLong(), anyLong(), any(), any(), any()))
                .thenThrow(new AccessDeniedException(GUARD_MESSAGE));

        mockMvc.perform(get("/coach/athletes/2/workout-logs").with(user(currentUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RESPONSE_MESSAGE));
    }

    @Test
    @DisplayName("cele bez relacji: 403")
    void shouldReturn403ForGoals() throws Exception {
        when(coachAthleteService.goals(anyLong(), anyLong(), any()))
                .thenThrow(new AccessDeniedException(GUARD_MESSAGE));

        mockMvc.perform(get("/coach/athletes/2/goals").with(user(currentUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RESPONSE_MESSAGE));
    }

    @Test
    @DisplayName("statystyki bez relacji: 403")
    void shouldReturn403ForStatistics() throws Exception {
        when(coachAthleteService.statistics(anyLong(), anyLong(), any(), any()))
                .thenThrow(new AccessDeniedException(GUARD_MESSAGE));

        mockMvc.perform(get("/coach/athletes/2/statistics").with(user(currentUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RESPONSE_MESSAGE));
    }

    @Test
    @DisplayName("agregat tygodniowy bez relacji: 403")
    void shouldReturn403ForWeeklyStatistics() throws Exception {
        when(coachAthleteService.weeklyStatistics(anyLong(), anyLong(), any(), any()))
                .thenThrow(new AccessDeniedException(GUARD_MESSAGE));

        mockMvc.perform(get("/coach/athletes/2/statistics/weekly")
                        .param("from", "2026-03-01").param("to", "2026-03-31")
                        .with(user(currentUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RESPONSE_MESSAGE));
    }

    @Test
    @DisplayName("lista podopiecznych przychodzi z danymi do wyświetlenia")
    void shouldReturnAthletesWithDisplayData() throws Exception {
        when(coachAthleteService.athletes(1L)).thenReturn(List.of(
                new AthleteResponse(2L, "Anna", "Nowak", "anna@example.com",
                        LocalDateTime.of(2026, 3, 1, 10, 0))));

        mockMvc.perform(get("/coach/athletes").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Anna"))
                .andExpect(jsonPath("$[0].email").value("anna@example.com"))
                .andExpect(jsonPath("$[0].cooperationSince").exists());
    }

    @Test
    @DisplayName("identyfikator z adresu trafia do serwisu jako podopieczny, a zalogowany jako trener")
    void shouldPassCoachAndAthleteInRightOrder() throws Exception {
        when(coachAthleteService.workoutLogs(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/coach/athletes/2/workout-logs")
                        .param("categoryId", "5")
                        .param("from", "2026-03-01").param("to", "2026-03-31")
                        .with(user(currentUser)))
                .andExpect(status().isOk());

        // Zamiana tych dwóch argumentów miejscami nie rzuciłaby wyjątkiem - trener
        // zobaczyłby własny dziennik pod adresem podopiecznego.
        verify(coachAthleteService).workoutLogs(eq(1L), eq(ATHLETE_ID), eq(5L),
                eq(LocalDate.of(2026, 3, 1)), eq(LocalDate.of(2026, 3, 31)));
    }

    @Test
    @DisplayName("statystyki bez zakresu obejmują bieżący miesiąc")
    void shouldDefaultToCurrentMonth() throws Exception {
        when(coachAthleteService.statistics(anyLong(), anyLong(), any(), any())).thenReturn(null);

        mockMvc.perform(get("/coach/athletes/2/statistics").with(user(currentUser)))
                .andExpect(status().isOk());

        YearMonth currentMonth = YearMonth.now();
        verify(coachAthleteService).statistics(eq(1L), eq(ATHLETE_ID),
                eq(currentMonth.atDay(1)), eq(currentMonth.atEndOfMonth()));
    }

    @Test
    @DisplayName("statystyki z jedną granicą zakresu: 400, bez sięgania do serwisu")
    void shouldReturn400ForPartialRange() throws Exception {
        mockMvc.perform(get("/coach/athletes/2/statistics")
                        .param("from", "2026-03-01")
                        .with(user(currentUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("obie granice")));

        verify(coachAthleteService, org.mockito.Mockito.never())
                .statistics(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("filtr stanu celów przechodzi do serwisu")
    void shouldPassGoalStatusFilter() throws Exception {
        when(coachAthleteService.goals(anyLong(), anyLong(), any())).thenReturn(List.of());

        mockMvc.perform(get("/coach/athletes/2/goals")
                        .param("status", "active")
                        .with(user(currentUser)))
                .andExpect(status().isOk());

        verify(coachAthleteService).goals(eq(1L), eq(ATHLETE_ID), eq(GoalStatus.ACTIVE));
    }

    @Test
    @DisplayName("tworzenie planu bez relacji: 403")
    void shouldReturn403WhenCreatingPlanWithoutCooperation() throws Exception {
        when(coachAthleteService.createTrainingPlan(anyLong(), anyLong(), any()))
                .thenThrow(new AccessDeniedException(GUARD_MESSAGE));

        mockMvc.perform(post("/coach/athletes/2/training-plans")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RESPONSE_MESSAGE));
    }

    @Test
    @DisplayName("edycja cudzego planu: 403 z konkretnym powodem")
    void shouldReturn403WithReasonWhenEditingForeignPlan() throws Exception {
        when(coachAthleteService.updateTrainingPlan(anyLong(), anyLong(), anyLong(), any()))
                .thenThrow(new PlanAuthorshipException());

        mockMvc.perform(put("/coach/athletes/2/training-plans/10")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Możesz edytować tylko plany, które sam ułożyłeś"));
    }

    @Test
    @DisplayName("edycja planu po zakończeniu współpracy: 403 z komunikatem, na którym front opiera wyjście z trybu")
    void shouldReturnGenericForbiddenWhenEditingWithoutCooperation() throws Exception {
        when(coachAthleteService.updateTrainingPlan(anyLong(), anyLong(), anyLong(), any()))
                .thenThrow(new AccessDeniedException(GUARD_MESSAGE));

        mockMvc.perform(put("/coach/athletes/2/training-plans/10")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RESPONSE_MESSAGE));
    }

    @Test
    @DisplayName("utworzony plan wraca z 201 i oznaczeniem autorstwa")
    void shouldReturn201WithAuthorship() throws Exception {
        TrainingPlanResponse created = new TrainingPlanResponse(
                10L, "Interwały", 5L, "Taniec", "#9B59B6", "music",
                LocalDate.now().plusDays(2), null, 45, null, PlanStatus.PLANNED,
                true, "Jan Kowalski", 1L);

        when(coachAthleteService.createTrainingPlan(eq(1L), eq(ATHLETE_ID), any())).thenReturn(created);

        mockMvc.perform(post("/coach/athletes/2/training-plans")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdByCoach").value(true))
                .andExpect(jsonPath("$.createdByName").value("Jan Kowalski"))
                .andExpect(jsonPath("$.createdById").value(1));

        verify(coachAthleteService).createTrainingPlan(1L, ATHLETE_ID, validRequest);
    }

}
