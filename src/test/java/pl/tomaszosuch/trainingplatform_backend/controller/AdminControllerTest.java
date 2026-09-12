package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pl.tomaszosuch.trainingplatform_backend.dto.response.AdminUserResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PageResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.AccountStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.AdminUserSort;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.LastAdminException;
import pl.tomaszosuch.trainingplatform_backend.exception.SelfDeactivationException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.AdminUserService;

@WebMvcTest(controllers = AdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(AdminControllerTest.TestConfig.class)
@DisplayName("AdminControllerTest")
class AdminControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    private User admin;
    private User regularUser;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L).email("admin@example.com").firstName("Administrator").lastName("Systemu")
                .role(Role.ADMIN).isActive(true)
                .build();

        regularUser = User.builder()
                .id(2L).email("jan@example.com").firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(true)
                .build();
    }

    private static AdminUserResponse account(boolean active) {
        return new AdminUserResponse(
                2L, "jan@example.com", "Jan", "Kowalski", Role.USER, active,
                LocalDateTime.of(2026, 3, 1, 10, 0));
    }

    private static PageResponse<AdminUserResponse> onePage() {
        return new PageResponse<>(List.of(account(true)), 0, 20, 1, 1);
    }

    private static MockHttpServletRequestBuilder patchStatus(Long id, String body) {
        return patch("/admin/users/{id}/status", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    // --- GET /admin/users (G1) ---

    @Test
    @DisplayName("zwraca wyłącznie dane konta — dokładnie siedem pól")
    void shouldReturnAccountDataOnly() throws Exception {
        when(adminUserService.findUsers(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(onePage());

        mockMvc.perform(get("/admin/users").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0]", aMapWithSize(7)))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].email").value("jan@example.com"))
                .andExpect(jsonPath("$.content[0].firstName").value("Jan"))
                .andExpect(jsonPath("$.content[0].lastName").value("Kowalski"))
                .andExpect(jsonPath("$.content[0].role").value("USER"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("bez parametrów: pierwsza strona po 20, sortowanie po nazwisku, wszystkie stany")
    void shouldApplyDefaults() throws Exception {
        when(adminUserService.findUsers(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(onePage());

        mockMvc.perform(get("/admin/users").with(user(admin)))
                .andExpect(status().isOk());

        verify(adminUserService).findUsers(null, null, AdminUserSort.LAST_NAME, 0, 20);
    }

    @Test
    @DisplayName("przekazuje wyszukiwanie, filtr stanu, sortowanie i stronę do serwisu")
    void shouldPassParametersToService() throws Exception {
        when(adminUserService.findUsers(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(onePage());

        mockMvc.perform(get("/admin/users").with(user(admin))
                        .param("search", "kowal")
                        .param("status", "INACTIVE")
                        .param("sort", "NEWEST")
                        .param("page", "2")
                        .param("size", "50"))
                .andExpect(status().isOk());

        verify(adminUserService).findUsers("kowal", AccountStatus.INACTIVE, AdminUserSort.NEWEST, 2, 50);
    }

    @Test
    @DisplayName("zwykły użytkownik dostaje 403 na liście")
    void shouldReturn403ForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(regularUser)))
                .andExpect(status().isForbidden());

        verify(adminUserService, never()).findUsers(any(), any(), any(), anyInt(), anyInt());
    }

    @ParameterizedTest(name = "{0}={1} → 400")
    @CsvSource({
            "size, 0",
            "size, 101",
            "page, -1",
            "sort, EMAIL",
            "status, DELETED"
    })
    @DisplayName("niepoprawny parametr daje 400, nie 500")
    void shouldReturn400ForInvalidParameter(String name, String value) throws Exception {
        mockMvc.perform(get("/admin/users").with(user(admin)).param(name, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(adminUserService, never()).findUsers(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("zmiana stanu zwraca konto w tym samym kształcie co lista")
    void shouldReturnUpdatedAccountInListShape() throws Exception {
        when(adminUserService.changeStatus(1L, 2L, AccountStatus.INACTIVE)).thenReturn(account(false));

        mockMvc.perform(patchStatus(2L, "{\"status\":\"INACTIVE\"}").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(7)))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("zwykły użytkownik dostaje 403 przy zmianie stanu")
    void shouldReturn403WhenRegularUserChangesStatus() throws Exception {
        mockMvc.perform(patchStatus(5L, "{\"status\":\"INACTIVE\"}").with(user(regularUser)))
                .andExpect(status().isForbidden());

        verify(adminUserService, never()).changeStatus(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("brak statusu daje 400 z błędem pola")
    void shouldReturn400WhenStatusMissing() throws Exception {
        mockMvc.perform(patchStatus(2L, "{}").with(user(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").value("Status jest wymagany"));
    }

    @Test
    @DisplayName("nieznana wartość statusu daje 400, nie 500")
    void shouldReturn400ForUnknownStatus() throws Exception {
        mockMvc.perform(patchStatus(2L, "{\"status\":\"DELETED\"}").with(user(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(adminUserService, never()).changeStatus(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("wyłączenie własnego konta: 403 z komunikatem o własnym koncie")
    void shouldReturn403ForSelfDeactivation() throws Exception {
        when(adminUserService.changeStatus(1L, 1L, AccountStatus.INACTIVE))
                .thenThrow(new SelfDeactivationException());

        mockMvc.perform(patchStatus(1L, "{\"status\":\"INACTIVE\"}").with(user(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Nie możesz wyłączyć własnego konta"));
    }

    @Test
    @DisplayName("wyłączenie ostatniego administratora: 403 z komunikatem o ostatnim koncie")
    void shouldReturn403ForLastActiveAdmin() throws Exception {
        String message = "To ostatnie aktywne konto administratora — po jego wyłączeniu nikt nie odzyska dostępu do panelu";
        when(adminUserService.changeStatus(1L, 3L, AccountStatus.INACTIVE))
                .thenThrow(new LastAdminException(message));

        mockMvc.perform(patchStatus(3L, "{\"status\":\"INACTIVE\"}").with(user(admin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(message));
    }
}