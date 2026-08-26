package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.LoginResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidCredentialsException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidInvitationException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.impl.AuthServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;

@WebMvcTest(controllers = AuthController.class,
    excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
        type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class))
@WithMockUser
@org.springframework.context.annotation.Import(AuthControllerTest.TestConfig.class)
@DisplayName("AuthControllerTest")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthServiceImpl authService;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private RegisterRequest validRegister;
    private LoginRequest validLogin;
    private UserResponse userResponse;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        validRegister = new RegisterRequest("Jan", "Kowalski", "jan.kowalski@example.com", "password", "token-zaproszenia");
        validLogin = new LoginRequest("jan.kowalski@example.com", "password");
        userResponse = new UserResponse(1L, "jan.kowalski@example.com", "Jan", "Kowalski", LocalDate.of(1990, 5, 14), Role.USER);
        loginResponse = new LoginResponse("token", 1L, "jan.kowalski@example.com", Role.USER);
    }

    @Test
    @DisplayName("powinien zwrócić 201 gdy rejestracja powiodła się")
    public void shouldReturn201WhenRegistrationSucceeds() throws Exception {
        //given
        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);
        
        // when & then
        mockMvc.perform(post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegister)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy email ma niepoprawny format")
    public void shouldReturn400WhenEmailIsInvalid() throws Exception {
        // given
        RegisterRequest invalidRequest = new RegisterRequest("Jan", "Kowalski", "invalid-email", "password", "token-zaproszenia");

        // when & then
        mockMvc.perform(post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }
    
    @Test
    @DisplayName("powinien zwrócić 400 gdy hasło jest za krótkie")
    public void shouldReturn400WhenPasswordIsTooShort() throws Exception {
        // given
        RegisterRequest invalidRequest = new RegisterRequest("Jan", "Kowalski", "jan.kowalski@example.com", "abc", "token-zaproszenia");

        // when & then
        mockMvc.perform(post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy email jest już zajęty")
    public void shouldReturn400WhenEmailIsTaken() throws Exception {
        // given
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Adres e-mail jest już zajęty: jan.kowalski@example.com"));

        // when & then
        mockMvc.perform(post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegister)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Adres e-mail jest już zajęty: jan.kowalski@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 200 z tokenem gdy logowanie powiodło się")
    public void shouldReturn200WithTokenWhenLoginSucceeds() throws Exception {
        // given
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // when & then
        mockMvc.perform(post("/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 401 gdy dane logowania są błędne")
    public void shouldReturn401WhenLoginIsInvalid() throws Exception {
        // given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        // when & then
        mockMvc.perform(post("/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Nieprawidłowy e-mail lub hasło"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy email jest pusty")
    public void shouldReturn400WhenEmailIsEmpty() throws Exception {
        // given
        LoginRequest invalidRequest = new LoginRequest("", "password");

        // when & then
        mockMvc.perform(post("/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy brakuje tokenu zaproszenia")
    public void shouldReturn400WhenTokenIsMissing() throws Exception {
        RegisterRequest withoutToken = new RegisterRequest(
                "Jan", "Kowalski", "jan.kowalski@example.com", "password", "");

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withoutToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.token").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 200 z adresem z zaproszenia")
    public void shouldReturn200WithInvitationEmail() throws Exception {
        when(authService.checkInvitation("abc123")).thenReturn(
                new InvitationCheckResponse("zapraszany@example.com",
                        LocalDateTime.now().plusDays(7)));

        mockMvc.perform(get("/auth/invitation").param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("zapraszany@example.com"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    @DisplayName("powinien zwrócić 400 dla nieważnego tokenu")
    public void shouldReturn400ForInvalidToken() throws Exception {
        when(authService.checkInvitation("zly-token"))
                .thenThrow(new InvalidInvitationException("Zaproszenie wygasło"));

        mockMvc.perform(get("/auth/invitation").param("token", "zly-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Zaproszenie wygasło"));
    }

}
