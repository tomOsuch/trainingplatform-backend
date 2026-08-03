package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.AccessDeniedException;

import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutLogRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutLogResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutLogNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.WorkoutLogService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = WorkoutLogController.class, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@WithMockUser
@DisplayName("WorkoutLogControllerTest")
public class WorkoutLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkoutLogService logService;

    private User currentUser;
    private WorkoutLogResponse logResponse;
    private WorkoutLogRequest validRequest;

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

        logResponse = new WorkoutLogResponse(
                10L, 5L, "Taniec", "#9B59B6", null,
                LocalDate.now(), 60, 7, "dobry trening");

        validRequest = new WorkoutLogRequest(
                5L, null, LocalDate.now(), 60, 7, "dobry trening");
    }

    @Test
    @DisplayName("powinien zwrócić 200 z listą wpisów użytkownika")
    public void shouldReturn200WithUserLogs() throws Exception {
        // given
        when(logService.getUserLogs(1L, null, null, null))
                .thenReturn(List.of(logResponse));

        mockMvc.perform(get("/workout-logs")
                .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].categoryName").value("Taniec"))
                .andExpect(jsonPath("$[0].intensity").value(7));

        verify(logService).getUserLogs(1L, null, null, null);
    }

    @Test
    @DisplayName("powinien przekazać filtr kategorii gdy podano categoryId")
    public void shouldPassCategoryFilterProvided() throws Exception {
        // given
        when(logService.getUserLogs(1L, 5L, null, null))
                .thenReturn(List.of(logResponse));

        // when & then
        mockMvc.perform(get("/workout-logs")
                .with(user(currentUser))
                .param("categoryId", "5"))
                .andExpect(status().isOk());

        verify(logService).getUserLogs(1L, 5L, null, null);

    }

    @Test
    @DisplayName("powinien przekazać zakres dat gdy podano from i to")
    public void shouldPassDateRangeWhenProvided() throws Exception {
        // given
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = LocalDate.now().plusDays(5);
        when(logService.getUserLogs(1L, null, from, to))
                .thenReturn(List.of(logResponse));

        // when & then
        mockMvc.perform(get("/workout-logs")
                .with(user(currentUser))
                .param("from", from.toString())
                .param("to", to.toString()))
                .andExpect(status().isOk());

        verify(logService).getUserLogs(1L, null, from, to);
    }

    @Test
    @DisplayName("powinien zwrócić 200 ze szczegółami wpisu")
    public void shouldReturn200WithLogDetails() throws Exception {
        // given
        when(logService.getById(1L, 10L)).thenReturn(logResponse);

        // when & then
        mockMvc.perform(get("/workout-logs/10").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.categoryName").value("Taniec"))
                .andExpect(jsonPath("$.intensity").value(7));

        verify(logService).getById(1L, 10L);
    }

    @Test
    @DisplayName("powinien zwrócić 404 gdy wpis nie istnieje")
    public void shouldReturn404WhenLogNotFound() throws Exception {
        // given
        when(logService.getById(1L, 99L))
                .thenThrow(new WorkoutLogNotFoundException(99L));

        // when & then
        mockMvc.perform(get("/workout-logs/99").with(user(currentUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("powinien zwrócić 403 gdy wpis należy do innego użytkownika")
    public void shouldReturn403WhenLogNotOwned() throws Exception {
        when(logService.getById(1L, 10L))
                .thenThrow(new AccessDeniedException("Brak uprawnień"));

        mockMvc.perform(get("/workout-logs/10")
                .with(user(currentUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("powinien zwrócić 201 gdy wpis został dodany")
    public void shouldReturn201WhenLogCreated() throws Exception {
        // given
        when(logService.create(eq(1L), any(WorkoutLogRequest.class)))
                .thenReturn(logResponse);

        // when & then
        mockMvc.perform(post("/workout-logs")
                .with(user(currentUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));

        verify(logService).create(eq(1L), any(WorkoutLogRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy brak kategorii")
    public void shouldReturn400WhenCategoryIsNull() throws Exception {

        WorkoutLogRequest invalid = new WorkoutLogRequest(
                null, null, LocalDate.now(), 60, 7, null);

        mockMvc.perform(post("/workout-logs")
                .with(user(currentUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.categoryId").exists());

        verify(logService, never()).create(anyLong(), any(WorkoutLogRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy data jest z przyszłości")
    public void shouldReturn400WhenDateInFuture() throws Exception {
        WorkoutLogRequest invalid = new WorkoutLogRequest(
                5L, null, LocalDate.now().plusDays(1), 60, 7, null);

        mockMvc.perform(post("/workout-logs")
                .with(user(currentUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.performedDate").exists());

        verify(logService, never()).create(anyLong(), any(WorkoutLogRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy czas trwania wynosi 0")
    public void shouldReturn400WhenDurationIsZero() throws Exception {
        WorkoutLogRequest invalid = new WorkoutLogRequest(
                5L, null, LocalDate.now(), 0, 7, null);

        mockMvc.perform(post("/workout-logs")
                .with(user(currentUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.durationMin").exists());

        verify(logService, never()).create(anyLong(), any(WorkoutLogRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 200 gdy wpis został zaktualizowany")
    void shouldReturn200WhenLogUpdated() throws Exception {
        when(logService.update(eq(1L), eq(10L), any(WorkoutLogRequest.class)))
                .thenReturn(logResponse);

        mockMvc.perform(put("/workout-logs/10")
                .with(user(currentUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(logService).update(eq(1L), eq(10L), any(WorkoutLogRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 403 gdy wpis należy do innego użytkownika")
    void shouldReturn403WhenNotOwned() throws Exception {
        when(logService.update(eq(1L), eq(10L), any(WorkoutLogRequest.class)))
                .thenThrow(new AccessDeniedException("Brak uprawnień"));

        mockMvc.perform(put("/workout-logs/10")
                .with(user(currentUser))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("powinien zwrócić 204 gdy wpis został usunięty")
    void shouldReturn204WhenLogDeleted() throws Exception {
        doNothing().when(logService).delete(1L, 10L);

        mockMvc.perform(delete("/workout-logs/10")
                .with(user(currentUser))
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(logService).delete(1L, 10L);
    }

}
