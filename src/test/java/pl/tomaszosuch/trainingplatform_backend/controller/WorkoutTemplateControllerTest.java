package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutTemplateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutTemplateResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutTemplateNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.WorkoutTemplateService;

@WebMvcTest(controllers = WorkoutTemplateController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(WorkoutTemplateControllerTest.TestConfig.class)
@DisplayName("WorkoutTemplateControllerTest")
class WorkoutTemplateControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WorkoutTemplateService workoutTemplateService;

    private User currentUser;
    private WorkoutTemplateResponse templateResponse;
    private WorkoutTemplateRequest validRequest;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L).email("jan@example.com").firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(true)
                .build();

        templateResponse = new WorkoutTemplateResponse(
                10L, "Wtorkowy trening", "Rozgrzewka i figury",
                5L, "Taniec", "#9B59B6", "music", 60);

        validRequest = new WorkoutTemplateRequest("Wtorkowy trening", "Rozgrzewka i figury", 5L, 60);
    }

    @Test
    @DisplayName("GET /workout-templates zwraca listę szablonów użytkownika")
    void shouldReturn200WithTemplates() throws Exception {
        when(workoutTemplateService.getTemplates(1L)).thenReturn(List.of(templateResponse));

        mockMvc.perform(get("/workout-templates").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Wtorkowy trening"))
                .andExpect(jsonPath("$[0].categoryIconName").value("music"))
                .andExpect(jsonPath("$[0].durationMin").value(60));
    }

    @Test
    @DisplayName("POST /workout-templates zwraca 201")
    void shouldReturn201WhenCreated() throws Exception {
        when(workoutTemplateService.createTemplate(eq(1L), any(WorkoutTemplateRequest.class)))
                .thenReturn(templateResponse);

        mockMvc.perform(post("/workout-templates")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("POST bez nazwy i kategorii zwraca 400 z oboma błędami")
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/workout-templates")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WorkoutTemplateRequest(null, null, null, 60))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists());

        verify(workoutTemplateService, never()).createTemplate(anyLong(), any());
    }

    @Test
    @DisplayName("POST z niedodatnim czasem trwania zwraca 400")
    void shouldReturn400WhenDurationNotPositive() throws Exception {
        mockMvc.perform(post("/workout-templates")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WorkoutTemplateRequest("Zero minut", null, 5L, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.durationMin").exists());
    }

    @Test
    @DisplayName("GET cudzego szablonu zwraca 403")
    void shouldReturn403WhenReadingForeignTemplate() throws Exception {
        when(workoutTemplateService.getTemplate(1L, 10L))
                .thenThrow(new AccessDeniedException("Brak uprawnień do tego szablonu"));

        mockMvc.perform(get("/workout-templates/10").with(user(currentUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("PUT cudzego szablonu zwraca 403")
    void shouldReturn403WhenUpdatingForeignTemplate() throws Exception {
        when(workoutTemplateService.updateTemplate(eq(1L), eq(10L), any(WorkoutTemplateRequest.class)))
                .thenThrow(new AccessDeniedException("Brak uprawnień do tego szablonu"));

        mockMvc.perform(put("/workout-templates/10")
                        .with(user(currentUser)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE cudzego szablonu zwraca 403")
    void shouldReturn403WhenDeletingForeignTemplate() throws Exception {
        doThrow(new AccessDeniedException("Brak uprawnień do tego szablonu"))
                .when(workoutTemplateService).deleteTemplate(1L, 10L);

        mockMvc.perform(delete("/workout-templates/10").with(user(currentUser)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE własnego szablonu zwraca 204")
    void shouldReturn204WhenDeleted() throws Exception {
        mockMvc.perform(delete("/workout-templates/10").with(user(currentUser)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(workoutTemplateService).deleteTemplate(1L, 10L);
    }

    @Test
    @DisplayName("GET nieistniejącego szablonu zwraca 404")
    void shouldReturn404WhenTemplateMissing() throws Exception {
        when(workoutTemplateService.getTemplate(1L, 99L))
                .thenThrow(new WorkoutTemplateNotFoundException(99L));

        mockMvc.perform(get("/workout-templates/99").with(user(currentUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}