package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImplTest")
public class AuthServiceImplTest {
    
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
            "Jan",
            "Kowalski",
            "jan.kowalski@example.com",
            "password"
        );

        savedUser = User.builder()
            .id(1L)
            .email(validRequest.email())
            .firstName(validRequest.firstName())
            .lastName(validRequest.lastName())
            .password("encodedPassword")
            .role(Role.USER)
            .isActive(true)
            .build();
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("powinien zarejestrować użytkownika gdy dane są poprawne")
        void shouldRegisterUserWhenDataIsValid() {
            // given
            when(userRepository.existsByEmail(validRequest.email()))
                .thenReturn(false);
            when(passwordEncoder.encode(validRequest.password()))
                .thenReturn("haslo_zahashowane");
            when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

            // when
            UserResponse response = authService.register(validRequest);

            // then
            assertNotNull(response);
            assertEquals(1L, response.id());
            assertEquals("jan.kowalski@example.com", response.email());
            assertEquals("Jan", response.firstName());
            assertEquals("Kowalski", response.lastName());
            assertEquals(Role.USER, response.role());

            verify(userRepository).existsByEmail(validRequest.email());
            verify(passwordEncoder).encode(validRequest.password());
            verify(userRepository).save(any(User.class));
        }
        @Test
        @DisplayName("powinien zahashować hasło przed zapisem")
        void shouldHashPasswordBeforeSaving() {
            // given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn("$2a$10$zahashowane");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // when
            authService.register(validRequest);

            // then
            verify(passwordEncoder).encode("password");
            verify(userRepository).save(argThat(user ->
                user.getPassword().equals("$2a$10$zahashowane")
            ));
        }

        @Test
        @DisplayName("powinien rzucić wyjątek gdy email jest już zajęty")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // given
            when(userRepository.existsByEmail(validRequest.email()))
                .thenReturn(true);

            // when & then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(validRequest)
            );

            assertTrue(exception.getMessage()
                .contains("jan.kowalski@example.com"));

            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("nie powinien zapisać użytkownika gdy email jest zajęty")
        void shouldNotSaveUserWhenEmailIsTaken() {
            // given
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // when
            assertThrows(IllegalArgumentException.class,
                () -> authService.register(validRequest));

            // then
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("nowy użytkownik powinien mieć rolę USER i aktywne konto")
        void shouldCreateUserWithDefaultRoleAndActiveAccount() {
            // given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // when
            authService.register(validRequest);

            // then
            verify(userRepository).save(argThat(user ->
                user.getRole() == Role.USER &&
                Boolean.TRUE.equals(user.getIsActive())
            ));
        }

        @Test
        @DisplayName("odpowiedź nie powinna zawierać hasła")
        void responseShouldNotContainPassword() {
            // given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // when
            UserResponse response = authService.register(validRequest);

            // then - record UserResponse nie ma pola password
            assertNotNull(response.id());
            assertNotNull(response.email());
            assertNotNull(response.firstName());
            assertNotNull(response.lastName());
            assertNotNull(response.role());
        }
    }


}
