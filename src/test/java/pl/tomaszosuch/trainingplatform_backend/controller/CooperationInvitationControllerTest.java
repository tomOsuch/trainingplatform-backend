package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationInvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationConflictException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.CooperationService;

@WebMvcTest(controllers = CooperationInvitationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(CooperationInvitationControllerTest.TestConfig.class)
@DisplayName("CooperationInvitationControllerTest")
class CooperationInvitationControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CooperationService cooperationService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L).email("trener@example.com").firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(true)
                .build();
    }

    private static CooperationInvitationResponse response(CooperationStatus status) {
        return new CooperationInvitationResponse(10L, 1L, "Jan", "Kowalski", "trener@example.com",
                status, LocalDateTime.now(), LocalDateTime.now().plusDays(14));
    }

    @Test
    @DisplayName("POST zwraca 201 z utworzonym zaproszeniem")
    void shouldReturn201OnInvite() throws Exception {
        when(cooperationService.invite(anyLong(), any())).thenReturn(response(CooperationStatus.PENDING));

        mockMvc.perform(post("/cooperation-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"podopieczny@example.com\"}")
                        .with(user(currentUser)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.coachEmail").value("trener@example.com"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("niepoprawny adres nie dochodzi do serwisu")
    void shouldReturn400ForInvalidEmail() throws Exception {
        mockMvc.perform(post("/cooperation-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"to-nie-jest-adres\"}")
                        .with(user(currentUser)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(cooperationService, never()).invite(anyLong(), any());
    }

    @Test
    @DisplayName("konflikt stanu współpracy zwraca 409 z komunikatem z serwisu")
    void shouldReturn409OnConflict() throws Exception {
        when(cooperationService.invite(anyLong(), any()))
                .thenThrow(new CooperationConflictException("Prowadzisz już tę osobę"));

        mockMvc.perform(post("/cooperation-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"podopieczny@example.com\"}")
                        .with(user(currentUser)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Prowadzisz już tę osobę"));
    }

    @Test
    @DisplayName("GET zwraca otrzymane zaproszenia")
    void shouldReturnReceivedInvitations() throws Exception {
        when(cooperationService.receivedInvitations(1L))
                .thenReturn(List.of(response(CooperationStatus.PENDING)));

        mockMvc.perform(get("/cooperation-invitations").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].coachFirstName").value("Jan"));
    }

    @Test
    @DisplayName("PATCH przyjmuje wyłącznie ACCEPTED i REJECTED")
    void shouldRejectUnknownDecision() throws Exception {
        mockMvc.perform(patch("/cooperation-invitations/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ENDED\"}")
                        .with(user(currentUser)).with(csrf()))
                .andExpect(status().isBadRequest());

        verify(cooperationService, never()).respond(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("PATCH zwraca współpracę po akceptacji")
    void shouldReturnActiveAfterAccept() throws Exception {
        when(cooperationService.respond(anyLong(), anyLong(), any()))
                .thenReturn(response(CooperationStatus.ACTIVE));

        mockMvc.perform(patch("/cooperation-invitations/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ACCEPTED\"}")
                        .with(user(currentUser)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}