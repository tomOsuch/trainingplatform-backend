package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.GoalNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.GoalService;

@WebMvcTest(controllers = GoalController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@WithMockUser
@Import(GoalControllerTest.TestConfig.class)
@DisplayName("GoalControllerTest")
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoalService goalService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    private User currentUser;
    private GoalResponse goalResponse;
    private GoalRequest validRequest;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L).email("jan@example.com").firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(true)
                .build();

        goalResponse = new GoalResponse(
                10L, "100 godzin tańca", null, 5L, "Taniec", "#9B59B6",
                GoalMetric.MINUTES, 6000, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                1500L, 25, false, null);

        validRequest = new GoalRequest(
                "100 godzin tańca", null, 5L, GoalMetric.MINUTES, 6000,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("GET /goals bez parametru zwraca 200 z listą i postępem")
    void shouldReturn200WithAllGoals() throws Exception {
        when(goalService.getGoals(eq(1L), isNull())).thenReturn(List.of(goalResponse));

        mockMvc.perform(get("/goals").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].metric").value("MINUTES"))
                .andExpect(jsonPath("$[0].currentValue").value(1500))
                .andExpect(jsonPath("$[0].percent").value(25))
                .andExpect(jsonPath("$[0].achieved").value(false));
    }

    @Test
    @DisplayName("GET /goals?status=active przekazuje filtr niezależnie od wielkości liter")
    void shouldPassStatusFilter() throws Exception {
        when(goalService.getGoals(1L, GoalStatus.ACTIVE)).thenReturn(List.of(goalResponse));

        mockMvc.perform(get("/goals").param("status", "active").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    @DisplayName("GET /goals?status=foo zwraca 400")
    void shouldReturn400ForUnknownStatus() throws Exception {
        mockMvc.perform(get("/goals").param("status", "foo").with(user(currentUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("foo")));

        verify(goalService, never()).getGoals(any(), any());
    }

    @Test
    @DisplayName("POST /goals zwraca 201")
    void shouldReturn201WhenCreated() throws Exception {
        when(goalService.createGoal(eq(1L), any(GoalRequest.class))).thenReturn(goalResponse);

        mockMvc.perform(post("/goals")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("POST /goals z wartością docelową 0 zwraca 400 z komunikatem z US-016")
    void shouldReturn400WhenTargetValueNotPositive() throws Exception {
        GoalRequest invalid = new GoalRequest("cel", null, null, GoalMetric.SESSIONS, 0, null, null);

        mockMvc.perform(post("/goals")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.targetValue").value("Wartość docelowa musi być większa od 0"));

        verify(goalService, never()).createGoal(any(), any());
    }

    @Test
    @DisplayName("POST /goals bez miary i tytułu zwraca 400 z oboma błędami")
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        GoalRequest invalid = new GoalRequest("", null, null, null, 10, null, null);

        mockMvc.perform(post("/goals")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.metric").exists());
    }

    @Test
    @DisplayName("PUT /goals/{id} zwraca 200 ze zaktualizowanym celem")
    void shouldReturn200WhenUpdated() throws Exception {
        when(goalService.updateGoal(eq(1L), eq(10L), any(GoalRequest.class))).thenReturn(goalResponse);

        mockMvc.perform(put("/goals/10")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("PUT /goals/{id} cudzego celu zwraca 403")
    void shouldReturn403WhenUpdatingForeignGoal() throws Exception {
        when(goalService.updateGoal(eq(1L), eq(10L), any(GoalRequest.class)))
                .thenThrow(new AccessDeniedException("Brak uprawnień do tego celu"));

        mockMvc.perform(put("/goals/10")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /goals/{id} cudzego celu zwraca 403")
    void shouldReturn403WhenDeletingForeignGoal() throws Exception {
        doThrow(new AccessDeniedException("Brak uprawnień do tego celu"))
                .when(goalService).deleteGoal(1L, 10L);

        mockMvc.perform(delete("/goals/10").with(user(currentUser)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /goals/{id} zwraca 204")
    void shouldReturn204WhenDeleted() throws Exception {
        mockMvc.perform(delete("/goals/10").with(user(currentUser)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(goalService).deleteGoal(1L, 10L);
    }

    @Test
    @DisplayName("PUT /goals/{id} nieistniejącego celu zwraca 404")
    void shouldReturn404WhenGoalMissing() throws Exception {
        when(goalService.updateGoal(eq(1L), eq(99L), any(GoalRequest.class)))
                .thenThrow(new GoalNotFoundException(99L));

        mockMvc.perform(put("/goals/99")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }

}