package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.hamcrest.Matchers.containsString;
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

import jakarta.servlet.http.Cookie;

import pl.tomaszosuch.trainingplatform_backend.config.RefreshTokenProperties;
import pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetConfirmRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.LoginResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PasswordResetCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidCredentialsException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidInvitationException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidPasswordResetTokenException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvalidRefreshTokenException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.security.RefreshTokenCookieFactory;
import pl.tomaszosuch.trainingplatform_backend.service.AuthService;
import pl.tomaszosuch.trainingplatform_backend.service.PasswordResetService;
import pl.tomaszosuch.trainingplatform_backend.service.RefreshTokenService;
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

    @MockitoBean
    private PasswordResetService passwordResetService;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @org.springframework.context.annotation.Bean
        RefreshTokenCookieFactory refreshTokenCookieFactory() {
            RefreshTokenProperties properties = new RefreshTokenProperties();
            properties.setExpirationDays(14);
            properties.setCookieName("refreshToken");
            properties.setCookiePath("/api/auth");
            properties.setCookieSecure(false);
            properties.setCookieSameSite("Lax");
            return new RefreshTokenCookieFactory(properties);
        }
    }

    private RegisterRequest validRegister;
    private LoginRequest validLogin;
    private UserResponse userResponse;
    private LoginResponse loginResponse;
    private AuthService.LoginResult loginResult;

    @BeforeEach
    void setUp() {
        validRegister = new RegisterRequest("Jan", "Kowalski", "jan.kowalski@example.com", "password", "token-zaproszenia");
        validLogin = new LoginRequest("jan.kowalski@example.com", "password");
        userResponse = new UserResponse(1L, "jan.kowalski@example.com", "Jan", "Kowalski", LocalDate.of(1990, 5, 14), Role.USER);
        loginResponse = new LoginResponse("token", 1L, "jan.kowalski@example.com", Role.USER);
        loginResult = new AuthService.LoginResult(
                loginResponse,
                new RefreshTokenService.IssuedToken("surowy-refresh-token", LocalDateTime.now().plusDays(14)));
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
        when(authService.login(any(LoginRequest.class), any())).thenReturn(loginResult);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=surowy-refresh-token")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));

        verify(authService).login(any(LoginRequest.class), any());
    }

    @Test
    @DisplayName("powinien zwrócić 401 gdy dane logowania są błędne")
    public void shouldReturn401WhenLoginIsInvalid() throws Exception {
        // given
        when(authService.login(any(LoginRequest.class), any()))
                .thenThrow(new InvalidCredentialsException());

        // when & then
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Nieprawidłowy e-mail lub hasło"));

        verify(authService).login(any(LoginRequest.class), any());
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

        verify(authService, never()).login(any(LoginRequest.class), any());
    }

    @Test
    @DisplayName("powinien zwrócić 200 i zrotowane ciasteczko przy odświeżeniu")
    public void shouldReturn200AndRotatedCookieOnRefresh() throws Exception {
        // given
        when(authService.refresh(any(), any())).thenReturn(loginResult);
        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", "stare-ciasteczko")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=surowy-refresh-token")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));

        verify(authService).refresh(any(), any());
    }

    @Test
    @DisplayName("powinien zwrócić 401, a nie 400, gdy brakuje ciasteczka")
    public void shouldReturn401WhenRefreshCookieIsMissing() throws Exception {
        // given
        when(authService.refresh(any(), any()))
                .thenThrow(new InvalidRefreshTokenException("Brak tokena odświeżającego"));
        // when & then
        mockMvc.perform(post("/auth/refresh").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Brak tokena odświeżającego"));
    }

    @Test
    @DisplayName("powinien zwrócić 401 dla unieważnionego tokena odświeżającego")
    public void shouldReturn401ForRevokedRefreshToken() throws Exception {
        // given
        when(authService.refresh(any(), any()))
                .thenThrow(new InvalidRefreshTokenException(
                        "Sesja została unieważniona ze względów bezpieczeństwa"));

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", "wykradzione")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("powinien zwrócić 204 i skasować ciasteczko przy wylogowaniu")
    public void shouldReturn204AndClearCookieOnLogout() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", "aktualne")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")));

        verify(authService).logout("aktualne");
    }

    @Test
    @DisplayName("wylogowanie bez ciasteczka też kończy się sukcesem")
    public void shouldReturn204OnLogoutWithoutCookie() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());

        verify(authService).logout(null);
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

    @Test
    @DisplayName("powinien zwrócić 202 przy żądaniu resetu hasła")
    public void shouldReturn202WhenPasswordResetRequested() throws Exception {
        mockMvc.perform(post("/auth/password-reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PasswordResetRequest("jan.kowalski@example.com"))))
                .andExpect(status().isAccepted());

        verify(passwordResetService).requestReset(any(PasswordResetRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy adres w żądaniu resetu jest niepoprawny")
    public void shouldReturn400WhenResetEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/password-reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PasswordResetRequest("to-nie-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(passwordResetService, never()).requestReset(any(PasswordResetRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 200 z adresem powiązanym z tokenem resetu")
    public void shouldReturn200WithResetTokenOwner() throws Exception {
        when(passwordResetService.checkToken("abc123")).thenReturn(
                new PasswordResetCheckResponse("jan.kowalski@example.com",
                        LocalDateTime.now().plusMinutes(60)));

        mockMvc.perform(get("/auth/password-reset").param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("powinien zwrócić 400 dla nieważnego tokenu resetu")
    public void shouldReturn400ForInvalidResetToken() throws Exception {
        when(passwordResetService.checkToken("zly-token"))
                .thenThrow(new InvalidPasswordResetTokenException("Link do resetu hasła wygasł"));

        mockMvc.perform(get("/auth/password-reset").param("token", "zly-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Link do resetu hasła wygasł"));
    }

    @Test
    @DisplayName("powinien zwrócić 204 po ustawieniu nowego hasła")
    public void shouldReturn204WhenPasswordChanged() throws Exception {
        mockMvc.perform(post("/auth/password-reset/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PasswordResetConfirmRequest("abc123", "NoweHaslo123"))))
                .andExpect(status().isNoContent());

        verify(passwordResetService).confirmReset(any(PasswordResetConfirmRequest.class));
    }

    @Test
    @DisplayName("powinien zwrócić 400 gdy nowe hasło jest za krótkie")
    public void shouldReturn400WhenNewPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/auth/password-reset/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PasswordResetConfirmRequest("abc123", "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(passwordResetService, never()).confirmReset(any(PasswordResetConfirmRequest.class));
    }

}