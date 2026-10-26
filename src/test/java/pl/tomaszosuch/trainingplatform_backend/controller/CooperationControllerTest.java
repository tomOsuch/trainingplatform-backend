package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationRole;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationConflictException;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.CooperationService;

@WebMvcTest(controllers = CooperationController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@Import(CooperationControllerTest.TestConfig.class)
@DisplayName("CooperationControllerTest")
class CooperationControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestConfig {
    }

    private static final Long COACH_ID = 1L;
    private static final Long ATHLETE_ID = 2L;

    private static final Long COOPERATION_ID = 10L;

    private static final String SERVICE_MESSAGE = "Nie jesteś stroną tej współpracy";
    private static final String RESPONSE_MESSAGE = "Brak uprawnień";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CooperationService cooperationService;

    private User coach;
    private User athlete;

    @BeforeEach
    void setUp() {
        coach = User.builder()
                .id(COACH_ID).email("trener@example.com").firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(true).coach(true)
                .build();
        athlete = User.builder()
                .id(ATHLETE_ID).email("podopieczna@example.com").firstName("Anna").lastName("Nowak")
                .role(Role.USER).isActive(true)
                .build();
    }

    @Test
    @DisplayName("GET: rola w odpowiedzi zależy od tego, kto pyta")
    void shouldReturnRoleOfRequester() throws Exception {
        when(cooperationService.activeCooperations(COACH_ID)).thenReturn(List.of(
                new CooperationResponse(COOPERATION_ID, CooperationRole.COACH, ATHLETE_ID,
                        "Anna", "Nowak", "podopieczna@example.com", LocalDateTime.now())));
        when(cooperationService.activeCooperations(ATHLETE_ID)).thenReturn(List.of(
                new CooperationResponse(COOPERATION_ID, CooperationRole.ATHLETE, COACH_ID,
                        "Jan", "Kowalski", "trener@example.com", LocalDateTime.now())));

        mockMvc.perform(get("/cooperations").with(user(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(COOPERATION_ID))
                .andExpect(jsonPath("$[0].role").value("COACH"));

        mockMvc.perform(get("/cooperations").with(user(athlete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(COOPERATION_ID))
                .andExpect(jsonPath("$[0].role").value("ATHLETE"));
    }

    @Test
    @DisplayName("GET: partner w odpowiedzi to druga strona relacji, nie pytający")
    void shouldReturnOtherPartyAsPartner() throws Exception {
        when(cooperationService.activeCooperations(COACH_ID)).thenReturn(List.of(
                new CooperationResponse(COOPERATION_ID, CooperationRole.COACH, ATHLETE_ID,
                        "Anna", "Nowak", "podopieczna@example.com", LocalDateTime.now())));

        mockMvc.perform(get("/cooperations").with(user(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partnerId").value(ATHLETE_ID))
                .andExpect(jsonPath("$[0].partnerFirstName").value("Anna"))
                .andExpect(jsonPath("$[0].partnerLastName").value("Nowak"))
                .andExpect(jsonPath("$[0].partnerEmail").value("podopieczna@example.com"))
                .andExpect(jsonPath("$[0].since").exists());
    }

    @Test
    @DisplayName("DELETE: zakończenie współpracy zwraca 204")
    void shouldReturn204OnEnd() throws Exception {
        mockMvc.perform(delete("/cooperations/{id}", COOPERATION_ID)
                        .with(user(coach)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(cooperationService).end(COACH_ID, COOPERATION_ID);
    }

    @Test
    @DisplayName("DELETE cudzej współpracy: 403 z komunikatem handlera, nie serwisu")
    void shouldReturn403WhenNotParticipant() throws Exception {
        doThrow(new AccessDeniedException(SERVICE_MESSAGE))
                .when(cooperationService).end(anyLong(), anyLong());

        mockMvc.perform(delete("/cooperations/{id}", COOPERATION_ID)
                        .with(user(athlete)).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RESPONSE_MESSAGE))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("DELETE współpracy już zakończonej: 409 z komunikatem z serwisu")
    void shouldReturn409WhenAlreadyEnded() throws Exception {
        doThrow(new CooperationConflictException("Ta współpraca nie jest aktywna"))
                .when(cooperationService).end(anyLong(), anyLong());

        mockMvc.perform(delete("/cooperations/{id}", COOPERATION_ID)
                        .with(user(coach)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ta współpraca nie jest aktywna"));
    }

    @Test
    @DisplayName("DELETE nieistniejącej współpracy: 404")
    void shouldReturn404WhenCooperationMissing() throws Exception {
        doThrow(new CooperationNotFoundException(COOPERATION_ID))
                .when(cooperationService).end(anyLong(), anyLong());

        mockMvc.perform(delete("/cooperations/{id}", COOPERATION_ID)
                        .with(user(coach)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Nie znaleziono zaproszenia o identyfikatorze " + COOPERATION_ID));
    }

}