package pl.tomaszosuch.trainingplatform_backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import pl.tomaszosuch.trainingplatform_backend.dto.request.TrainingPlanRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.TrainingPlanNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.TrainingPlanService;

@WebMvcTest(controllers = TrainingPlanController.class, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@WithMockUser
@org.springframework.context.annotation.Import(TrainingPlanControllerTest.TestConfig.class)
@DisplayName("TrainingPlanControllerTest")
public class TrainingPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TrainingPlanService trainingPlanService;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    private User currentUser;
    private TrainingPlanResponse planResponse;
    private TrainingPlanRequest validRequest;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .email("jan@example.com")
                .firstName("Jan")
                .lastName("Kowalski")
                .role(Role.USER)
                .isActive(true)
                .build();

        planResponse = new TrainingPlanResponse(
                10L, "Salsa wieczorna", 5L, "Taniec", "#9B59B6",
                LocalDate.now().plusDays(3), null, 60, null, PlanStatus.PLANNED);

        validRequest = new TrainingPlanRequest(
                "Salsa wieczorna", 5L,
                LocalDate.now().plusDays(3), null, 60, null);
    }

    @Test
    @DisplayName("powinien zwrócić 200 z listą planów użytkownika")
    public void shouldReturn200WithUserPlans() throws Exception {
        // given
        when(trainingPlanService.getTrainingPlansByUserId(1L, null, null)).thenReturn(List.of(planResponse));

        // when
        mockMvc.perform(get("/training-plans").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Salsa wieczorna"))
                .andExpect(jsonPath("$[0].categoryId").value(5))
                .andExpect(jsonPath("$[0].categoryName").value("Taniec"))
                .andExpect(jsonPath("$[0].categoryColor").value("#9B59B6"))
                .andExpect(jsonPath("$[0].plannedDate").value(LocalDate.now().plusDays(3).toString()))
                .andExpect(jsonPath("$[0].durationMin").value(60))
                .andExpect(jsonPath("$[0].status").value(PlanStatus.PLANNED.name()));
    }

    @Test
    @DisplayName("powinien zwrócić 200 ze szczegółami planu")
    public void shouldReturn200WithPlanDetails() throws Exception {
        // given
        when(trainingPlanService.getTrainingPlanById(10L, 1L)).thenReturn(planResponse);

        // when
        mockMvc.perform(get("/training-plans/10").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Salsa wieczorna"))
                .andExpect(jsonPath("$.categoryId").value(5))
                .andExpect(jsonPath("$.categoryName").value("Taniec"))
                .andExpect(jsonPath("$.categoryColor").value("#9B59B6"))
                .andExpect(jsonPath("$.plannedDate").value(LocalDate.now().plusDays(3).toString()))
                .andExpect(jsonPath("$.durationMin").value(60))
                .andExpect(jsonPath("$.status").value(PlanStatus.PLANNED.name()));
    }

    @Test
    @DisplayName("powinien zwrócić 404 gdy plan nie istnieje")
    public void shouldReturn404WhenPlanNotFound() throws Exception {
        when(trainingPlanService.getTrainingPlanById(99L, 1L))
                .thenThrow(new TrainingPlanNotFoundException(99L));

        // when
        mockMvc.perform(get("/training-plans/99").with(user(currentUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("powinien zwrócić 403 gdy plan należy do innego użytkownika")
    public void shouldReturn403WhenPlanNotOwned() throws Exception {
        when(trainingPlanService.getTrainingPlanById(10L, 1L))
                .thenThrow(new AccessDeniedException("Brak uprawnień"));

        mockMvc.perform(get("/training-plans/10")
                .with(user(currentUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("powinien zwrócić 201 gdy plan został utworzony")
    public void shouldReturn201WhenPlanCreated() throws Exception {
        when(trainingPlanService.createTrainingPlan(eq(1L), any(TrainingPlanRequest.class)))
                .thenReturn(planResponse);

        mockMvc.perform(post("/training-plans")
                .with(user(currentUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Salsa wieczorna"))
                .andExpect(jsonPath("$.status").value(PlanStatus.PLANNED.name()));

        verify(trainingPlanService).createTrainingPlan(1L, validRequest);
    }

}
