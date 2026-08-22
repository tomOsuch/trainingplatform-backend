package pl.tomaszosuch.trainingplatform_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.tomaszosuch.trainingplatform_backend.dto.request.ChangePasswordRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.UpdateProfileRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.UserService;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

@WebMvcTest(controllers = UserController.class, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@WithMockUser
@DisplayName("UserControllerTest")
public class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private UserService userService;

        private User currentUser;
        private UserResponse userResponse;

        @BeforeEach
        void setUp() {
                currentUser = User.builder()
                                .id(1L)
                                .email("jankowalski@example.com")
                                .firstName("Jan")
                                .lastName("Kowalski")
                                .role(Role.USER)
                                .isActive(true)
                                .build();

                userResponse = new UserResponse(
                                1L,
                                "jan@example.com",
                                "Jan",
                                "Kowalski",
                                Role.USER);
        }

        @Test
        @DisplayName("powinien zrobic 200 z profilem zalogowanego uzytkownika")
        public void shouldReturnUserProfileWhenUserIsAuthenticated() throws Exception {
                // given
                when(userService.getUserProfile(1L)).thenReturn(userResponse);

                // when & then
                mockMvc.perform(get("/users")
                                .with(user(currentUser)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1L))
                                .andExpect(jsonPath("$.email").value("jan@example.com"))
                                .andExpect(jsonPath("$.firstName").value("Jan"))
                                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                                .andExpect(jsonPath("$.role").value("USER"));

                verify(userService).getUserProfile(1L);
        }

        @Test
        @DisplayName("powinien zrobic 404 gdy uzytkownik nie istnieje")
        public void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {

                // given
                when(userService.getUserProfile(1L)).thenThrow(new UserNotFoundException("User not found"));

                // when & then
                mockMvc.perform(get("/users")
                                .with(user(currentUser)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("powinien zwrocic 200 gdy profil zostaje zaktualizowany")
        public void shouldReturnOkWhenUserProfileIsUpdated() throws Exception {
        
                // given
                UpdateProfileRequest request = new UpdateProfileRequest(
                        "Anna", "Nowak", LocalDate.of(1990, 5, 14)
                );

                when(userService.updateUserProfile(eq(1L), any(UpdateProfileRequest.class)))
                        .thenReturn(userResponse);

                // when & then
                mockMvc.perform(put("/users")
                                .with(user(currentUser))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1L));

                verify(userService).updateUserProfile(eq(1L), any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("powinien zwrocic 400 gdy imie jest puste podczas aktualizacji profilu")
        public void shouldReturnBadRequestWhenFirstNameIsEmptyDuringProfileUpdate() throws Exception {
                // given
                UpdateProfileRequest request = new UpdateProfileRequest(
                        "", "Nowak", LocalDate.of(1990, 5, 14)
                );

                // when & then
                mockMvc.perform(put("/users")
                                .with(user(currentUser))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));

                verify(userService, never()).updateUserProfile(anyLong(), any(UpdateProfileRequest.class));
        }

        @Test
        @DisplayName("powinien zwrocic 400 gdy nazwisko jest puste podczas aktualizacji profilu")
        public void shouldReturnBadRequestWhenLastNameIsEmptyDuringProfileUpdate() throws Exception {
                // given
                UpdateProfileRequest request = new UpdateProfileRequest(
                        "Anna", "", LocalDate.of(1990, 5, 14)
                );

                // when & then
                mockMvc.perform(put("/users")
                                .with(user(currentUser))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));    

                verify(userService, never()).updateUserProfile(anyLong(), any(UpdateProfileRequest.class));
        }

          @Test
        @DisplayName("powinien zwrócić 400 gdy data urodzenia jest w przyszłości")
        void shouldReturn400WhenBirthDateIsInFuture() throws Exception {
            // given
            UpdateProfileRequest invalidRequest = new UpdateProfileRequest(
                "Anna", "Nowak", LocalDate.now().plusDays(1)
            );

            // when & then
            mockMvc.perform(put("/users")
                    .with(user(currentUser))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birthDate").exists());

            verify(userService, never()).updateUserProfile(any(), any());
        }

        @Test
        @DisplayName("powinien zwrócić 404 gdy użytkownik nie istnieje podczas tworzenia profilu")
        void shouldReturn404WhenUserNotFoundDuringProfileCreation() throws Exception {
            // given
            UpdateProfileRequest request = new UpdateProfileRequest(
                "Anna", "Nowak", null);

            when(userService.updateUserProfile(eq(1L), any(UpdateProfileRequest.class)))
                .thenThrow(new UserNotFoundException(1L));

            // when & then
            mockMvc.perform(put("/users")
                    .with(user(currentUser))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("powinien zwrócić 200 gdy hasło zostało zmienione")
        void shouldReturn200WhenPasswordChanged() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest(
                "stareHaslo", "noweHaslo123", "noweHaslo123");

            doNothing().when(userService)
                .changePassword(eq(1L), any(ChangePasswordRequest.class));

            // when & then
            mockMvc.perform(post("/users/change-password")
                    .with(user(currentUser))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            verify(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("powinien zwrócić 400 gdy aktualne hasło jest puste")
        void shouldReturn400WhenCurrentPasswordIsBlank() throws Exception {
            // given
            ChangePasswordRequest invalidRequest = new ChangePasswordRequest(
                "", "noweHaslo123", "noweHaslo123");

            // when & then
            mockMvc.perform(post("/users/change-password")
                    .with(user(currentUser))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").exists());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("powinien zwrócić 400 gdy nowe hasło jest za krótkie")
        void shouldReturn400WhenNewPasswordIsTooShort() throws Exception {
            // given
            ChangePasswordRequest invalidRequest = new ChangePasswordRequest(
                "stareHaslo", "abc", "abc");

            // when & then
            mockMvc.perform(post("/users/change-password")
                    .with(user(currentUser))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());

            verify(userService, never()).changePassword(any(), any());
        }

        @Test
        @DisplayName("powinien zwrócić 400 gdy hasła nie są zgodne")
        void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest(
                "stareHaslo", "noweHaslo123", "inneHaslo123");

            doThrow(new IllegalArgumentException("Hasła nie są zgodne"))
                .when(userService).changePassword(eq(1L), any());

            // when & then
            mockMvc.perform(post("/users/change-password")
                    .with(user(currentUser))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hasła nie są zgodne"));
        }

        @Test
        @DisplayName("powinien zwrócić 400 gdy nowe hasło jest takie samo jak stare")
        void shouldReturn400WhenNewPasswordSameAsOld() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest(
                "stareHaslo", "stareHaslo", "stareHaslo");

            doThrow(new IllegalArgumentException(
                    "Nowe hasło musi różnić się od aktualnego"))
                .when(userService).changePassword(eq(1L), any());

            // when & then
            mockMvc.perform(post("/users/change-password")
                    .with(user(currentUser))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                    .value("Nowe hasło musi różnić się od aktualnego"));
        }

        @Test
        @DisplayName("powinien zwrócić 404 gdy użytkownik nie istnieje")
        void shouldReturn404WhenUserNotFound() throws Exception {
            // given
            ChangePasswordRequest request = new ChangePasswordRequest(
                "stareHaslo", "noweHaslo123", "noweHaslo123");

            doThrow(new UserNotFoundException(1L))
                .when(userService).changePassword(eq(1L), any());

            // when & then
            mockMvc.perform(post("/users/change-password")
                    .with(user(currentUser))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
}
