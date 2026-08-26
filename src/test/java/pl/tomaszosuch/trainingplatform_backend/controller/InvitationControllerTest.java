package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.InvitationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailAlreadyRegisteredException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvitationAlreadyResolvedException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvitationNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.InvitationService;

@WebMvcTest(controllers = InvitationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(InvitationControllerTest.TestConfig.class)
@DisplayName("InvitationControllerTest")
public class InvitationControllerTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvitationService invitationService;

    private User admin;
    private User regularUser;
    private InvitationResponse invitationResponse;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L)
                .email("admin@example.com")
                .firstName("Administrator")
                .lastName("Systemu")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        regularUser = User.builder()
                .id(2L)
                .email("jan@example.com")
                .firstName("Jan")
                .lastName("Kowalski")
                .role(Role.USER)
                .isActive(true)
                .build();

        invitationResponse = new InvitationResponse(
                10L,
                "nowy@example.com",
                Role.USER,
                InvitationStatus.PENDING,
                "admin@example.com",
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    @Test
    @DisplayName("powinien zwrócić 201 przy wystawieniu zaproszenia przez administratora")
    void shouldReturn201WhenAdminCreatesInvitation() throws Exception {
        when(invitationService.createInvitation(any(InvitationRequest.class), eq(1L)))
                .thenReturn(invitationResponse);

        mockMvc.perform(post("/invitations")
                        .with(user(admin))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InvitationRequest("nowy@example.com", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.email").value("nowy@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.invitedByEmail").value("admin@example.com"));
    }

    @Test
    @DisplayName("nie powinien ujawniać tokenu w odpowiedzi")
    void shouldNotExposeTokenInResponse() throws Exception {
        when(invitationService.createInvitation(any(InvitationRequest.class), eq(1L)))
                .thenReturn(invitationResponse);

        mockMvc.perform(post("/invitations")
                        .with(user(admin))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InvitationRequest("nowy@example.com", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist());
    }

    @Test
    @DisplayName("powinien zwrócić 403, gdy zaproszenie wystawia zwykły użytkownik")
    void shouldReturn403WhenRegularUserCreatesInvitation() throws Exception {
        mockMvc.perform(post("/invitations")
                        .with(user(regularUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InvitationRequest("nowy@example.com", null))))
                .andExpect(status().isForbidden());

        verify(invitationService, never()).createInvitation(any(), any());
    }

    @Test
    @DisplayName("powinien zwrócić 400 przy niepoprawnym adresie e-mail")
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/invitations")
                        .with(user(admin))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InvitationRequest("to-nie-jest-email", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(invitationService, never()).createInvitation(any(), any());
    }

    @Test
    @DisplayName("powinien zwrócić 409, gdy adres ma już konto")
    void shouldReturn409WhenEmailAlreadyRegistered() throws Exception {
        when(invitationService.createInvitation(any(InvitationRequest.class), eq(1L)))
                .thenThrow(new EmailAlreadyRegisteredException("nowy@example.com"));

        mockMvc.perform(post("/invitations")
                        .with(user(admin))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InvitationRequest("nowy@example.com", null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("powinien zwrócić 200 z listą zaproszeń")
    void shouldReturn200WithInvitationList() throws Exception {
        when(invitationService.findAllInvitations()).thenReturn(List.of(invitationResponse));

        mockMvc.perform(get("/invitations").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("powinien zwrócić 403 przy liście zaproszeń dla zwykłego użytkownika")
    void shouldReturn403WhenRegularUserListsInvitations() throws Exception {
        mockMvc.perform(get("/invitations").with(user(regularUser)))
                .andExpect(status().isForbidden());

        verify(invitationService, never()).findAllInvitations();
    }

    @Test
    @DisplayName("powinien zwrócić 204 przy unieważnieniu zaproszenia")
    void shouldReturn204WhenInvitationRevoked() throws Exception {
        doNothing().when(invitationService).revokeInvitation(10L);

        mockMvc.perform(delete("/invitations/10")
                        .with(user(admin))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(invitationService).revokeInvitation(10L);
    }

    @Test
    @DisplayName("powinien zwrócić 404 przy unieważnieniu nieistniejącego zaproszenia")
    void shouldReturn404WhenInvitationDoesNotExist() throws Exception {
        doThrow(new InvitationNotFoundException(99L))
                .when(invitationService).revokeInvitation(99L);

        mockMvc.perform(delete("/invitations/99")
                        .with(user(admin))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("powinien zwrócić 409 przy unieważnieniu wykorzystanego zaproszenia")
    void shouldReturn409WhenInvitationAlreadyUsed() throws Exception {
        doThrow(new InvitationAlreadyResolvedException(
                "Zaproszenie zostało już wykorzystane i nie można go unieważnić"))
                .when(invitationService).revokeInvitation(10L);

        mockMvc.perform(delete("/invitations/10")
                        .with(user(admin))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}