package pl.tomaszosuch.trainingplatform_backend.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import pl.tomaszosuch.trainingplatform_backend.controller.WorkoutCategoryController;
import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutCategoryRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutCategoryResponse;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WorkoutCategoryController.class, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(WorkoutCategoryControllerTest.MethodSecurityConfig.class)
@DisplayName("WorkoutCategoryControllerTest")
public class WorkoutCategoryControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WorkoutCategoryService workoutCategoryService;

    private WorkoutCategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        categoryResponse = new WorkoutCategoryResponse(1L, "Taniec", "#9B59B6", "dance");

    }

    @Test
    @WithMockUser
    @DisplayName("powinien zwrócić 200 z listą kategorii dla zalogowanego użytkownika")
    void shouldReturn200WithListOfCategoriesForAuthenticatedUser() throws Exception {
        // given
        when(workoutCategoryService.getAllCategories()).thenReturn(List.of(categoryResponse));

        // when & then
        mockMvc.perform(get("/workout-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Taniec"))
                .andExpect(jsonPath("$[0].color").value("#9B59B6"))
                .andExpect(jsonPath("$[0].iconName").value("dance"));
    }

    @Test
    @DisplayName("powinien zwrócić 401 gdy użytkownik nie jest zalogowany")
    void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {
        // when & then
        mockMvc.perform(get("/workout-categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("powinien zwrócić 200 z kategorią")
    void shouldReturn200WithCategory() throws Exception {
        // given
        when(workoutCategoryService.getCategoryById(eq(1L))).thenReturn(categoryResponse);

        // when & then
        mockMvc.perform(get("/workout-categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Taniec"))
                .andExpect(jsonPath("$.color").value("#9B59B6"))
                .andExpect(jsonPath("$.iconName").value("dance"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("powinien zwrócić 201 gdy admin tworzy kategorię")
    void shouldReturn201WhenAdminCreatesCategory() throws Exception {
        // given
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Joga", "#3498DB", "yoga");

        when(workoutCategoryService.createCategory(any(WorkoutCategoryRequest.class)))
                .thenReturn(categoryResponse);

        // when & then
        mockMvc.perform(post("/workout-categories")
                .with(csrf())
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Taniec"))
                .andExpect(jsonPath("$.color").value("#9B59B6"))
                .andExpect(jsonPath("$.iconName").value("dance"));

        verify(workoutCategoryService).createCategory(any(WorkoutCategoryRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 403 gdy zwykły użytkownik próbuje utworzyć kategorię")
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserTriesToCreateCategory() throws Exception {
        // given
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Joga", "#3498DB", "yoga");

        // when & then
        mockMvc.perform(post("/workout-categories")
                .with(csrf())
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(workoutCategoryService, never()).createCategory(any(WorkoutCategoryRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy nazwa jest pusta")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenNameIsEmpty() throws Exception {
        // given
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "", "#3498DB", "yoga");

        // when & then
        mockMvc.perform(post("/workout-categories")
                .with(csrf())
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(workoutCategoryService, never()).createCategory(any(WorkoutCategoryRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy kolor ma niepoprawny format")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenColorIsInvalid() throws Exception {
        // given
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Joga", "#12", "yoga");

        // when & then
        mockMvc.perform(post("/workout-categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").exists());
    }

    @Test
    @DisplayName("powinien zwrócić 200 gdy admin aktualizuje kategorię")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200WhenAdminUpdates() throws Exception {
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Taniec nowoczesny", "#9B59B6", "dance");

        when(workoutCategoryService.updateCategory(eq(1L), any(WorkoutCategoryRequest.class)))
                .thenReturn(categoryResponse);

        mockMvc.perform(put("/workout-categories/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("powinien zwrócić 403 gdy zwykły użytkownik aktualizuje")
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenRegularUserUpdates() throws Exception {
        WorkoutCategoryRequest request = new WorkoutCategoryRequest(
                "Taniec", "#9B59B6", "dance");

        mockMvc.perform(put("/workout-categories/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("powinien zwrócić 204 gdy admin usuwa kategorię")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn204WhenAdminDeletes() throws Exception {
        doNothing().when(workoutCategoryService).deleteCategory(1L);

        mockMvc.perform(delete("/workout-categories/1")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(workoutCategoryService).deleteCategory(1L);
    }

    @Test
    @DisplayName("powinien zwrócić 403 gdy zwykły użytkownik usuwa")
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenRegularUserDeletes() throws Exception {
        mockMvc.perform(delete("/workout-categories/1")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(workoutCategoryService, never()).deleteCategory(any());
    }
}
