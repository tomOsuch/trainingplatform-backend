package pl.tomaszosuch.trainingplatform_backend.security;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszosuch.trainingplatform_backend.config.SecurityConfig;
import pl.tomaszosuch.trainingplatform_backend.controller.ProfileController;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.service.ProfileService;

@WebMvcTest(controllers = ProfileController.class)
@Import(SecurityConfig.class)
@DisplayName("JwtAuthenticationFilterTest")
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "wazny-token-z-naglowka";
    private static final String EMAIL = "jan@example.com";
    private static final Long USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockitoBean
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(TOKEN)).thenReturn(EMAIL);
    }

    private static User account(boolean active) {
        return User.builder()
                .id(USER_ID).email(EMAIL).password("hash")
                .firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(active)
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions getProfileWithToken() throws Exception {
        return mockMvc.perform(get("/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN));
    }

    @Test
    @DisplayName("konto aktywne: token uwierzytelnia, żądanie przechodzi")
    void shouldAuthenticateActiveAccount() throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(account(true));
        when(profileService.getProfile(USER_ID)).thenReturn(
                new UserResponse(USER_ID, EMAIL, "Jan", "Kowalski", null, Role.USER));

        getProfileWithToken()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    @DisplayName("konto wyłączone: ten sam token daje 401, nie czeka na wygaśnięcie")
    void shouldRejectTokenOfDisabledAccount() throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(account(false));

        getProfileWithToken()
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Wymagane uwierzytelnienie"));

        verify(profileService, never()).getProfile(anyLong());
    }

    @Test
    @DisplayName("konto usunięte: 401, a nie 500 z wyjątku w filtrze")
    void shouldRejectTokenOfDeletedAccount() throws Exception {
        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenThrow(new UserNotFoundException("User not found with email: " + EMAIL));

        getProfileWithToken()
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(profileService, never()).getProfile(anyLong());
    }

    @Test
    @DisplayName("bez nagłówka Authorization: 401 i brak zapytania o konto")
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());

        verify(userDetailsService, never()).loadUserByUsername(EMAIL);
    }
}