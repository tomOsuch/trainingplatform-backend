package pl.tomaszosuch.trainingplatform_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pl.tomaszosuch.trainingplatform_backend.dto.request.ChangePasswordRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.DeleteAccountRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.UpdateProfileRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.LastAdminException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.InvitationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.ProfileServiceImpl;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImplTest")
public class ProfileServiceImplTest {

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .email("jan@example.com")
                .password("$2a$10$zahashowane")
                .firstName("Jan")
                .lastName("Kowalski")
                .role(Role.USER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("powinien zwrocic profil gdy uzytkownik istnieje")
    public void shouldReturnUserProfileWhenUserExists() {
        // given
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(existingUser));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(new UserResponse(1L, "jan@example.com", "Jan", "Kowalski", LocalDate.of(1990, 5, 14), Role.USER));

        // when
        UserResponse response = profileService.getProfile(1L);

        // then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("jan@example.com", response.email());
        assertEquals("Jan", response.firstName());
        assertEquals("Kowalski", response.lastName());
    }

    @Test
    @DisplayName("powinien rzucic wyjatek gdy urzytk,ownik nie istnieje")
    public void shouldThrowExceptionWhenUserDoesNotExist() {
        // given
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> profileService.getProfile(99L));
    }

    @Test
    @DisplayName("powinien zaktualizowac dane profilu")
    public void shouldUpdateUserProfileData() {
        // given
        UpdateProfileRequest updateRequest = new UpdateProfileRequest("Jan", "Kowalski", java.time.LocalDate.now());
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(new UserResponse(1L, "jan@example.com", "Jan", "Kowalski", LocalDate.of(1990, 5, 14), Role.USER));

        // when
        UserResponse updatedProfile = profileService.updateProfile(1L, updateRequest);

        // then
        assertNotNull(updatedProfile);
        assertEquals("Jan", updatedProfile.firstName());
        assertEquals("Kowalski", updatedProfile.lastName());
    }

    @Test
    @DisplayName("powinien rzucic wyjatek gdy uzytkownik do aktualizacji nie istnieje")
    public void shouldThrowExceptionWhenUpdatingNonExistingUser() {
        // give
        UpdateProfileRequest updateRequest = new UpdateProfileRequest("Jan", "Kowalski", java.time.LocalDate.now());
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class, () -> profileService.updateProfile(99L, updateRequest));
    }

    @Test
    @DisplayName("powinien zmienic haslo uzytkownika gdy dane sa poprawne")
    public void shouldChangeUserPasswordWhenDataIsCorrect() {
        // given
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest("stareHaslo", "newSecurePassword",
                "newSecurePassword");
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(existingUser));
        // stare haslo pasuje do zapisanego
        when(passwordEncoder.matches("stareHaslo", existingUser.getPassword())).thenReturn(true);
        // nowe haslo rozni sie od aktualnego
        when(passwordEncoder.matches("newSecurePassword", existingUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newSecurePassword")).thenReturn("$2a$10$noweZahashowane");

        // when
        profileService.changePassword(1L, changePasswordRequest);

        // then
        verify(userRepository).save(argThat(user -> user.getPassword().equals("$2a$10$noweZahashowane")));
    }

    @Test
    @DisplayName("powinien rzucic wyjatek gey aktualne haslo jest niepoprawne")
    public void shouldThrowExceptionWhenCurrentPasswordIsIncorrect() {
        // given
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest("niepoprawneHaslo", "newSecurePassword",
                "newSecurePassword");
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(existingUser));
        when(passwordEncoder.matches("niepoprawneHaslo", existingUser.getPassword())).thenReturn(false);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(1L, changePasswordRequest));

        assertEquals("Podane hasło jest nieprawidłowe", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy hasła nie są zgodne")
    void shouldThrowExceptionWhenPasswordsDoNotMatch() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest(
                "stareHaslo", "noweHaslo123", "inneHaslo123");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("stareHaslo", existingUser.getPassword()))
                .thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(1L, request));

        assertEquals("Hasła nie są zgodne", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy nowe hasło jest takie samo jak stare")
    void shouldThrowExceptionWhenNewPasswordSameAsOld() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest(
                "stareHaslo", "stareHaslo", "stareHaslo");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("stareHaslo", existingUser.getPassword()))
                .thenReturn(true);
        when(passwordEncoder.matches("stareHaslo", existingUser.getPassword()))
                .thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(1L, request));

        assertEquals("Nowe hasło musi różnić się od aktualnego",
                exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("powinien rzucić wyjątek gdy użytkownik nie istnieje")
    void shouldThrowExceptionWhenUserNotFound() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest(
                "stareHaslo", "noweHaslo123", "noweHaslo123");

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(UserNotFoundException.class,
                () -> profileService.changePassword(99L, request));

        verify(userRepository, never()).save(any());
    }

    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccountTests {

        private static final String PASSWORD = "MojeHaslo123";

        private User accountOwner;

        @BeforeEach
        void prepareOwner() {
            accountOwner = User.builder()
                    .id(3L)
                    .email("jan@example.com")
                    .password("$2a$10$zahashowane")
                    .firstName("Jan")
                    .lastName("Kowalski")
                    .role(Role.USER)
                    .isActive(true)
                    .build();
        }

        @Test
        @DisplayName("usuwa konto po potwierdzeniu hasłem")
        void shouldDeleteAccountWhenPasswordMatches() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(accountOwner));
            when(passwordEncoder.matches(PASSWORD, "$2a$10$zahashowane")).thenReturn(true);

            profileService.deleteAccount(3L, new DeleteAccountRequest(PASSWORD));

            verify(userRepository).delete(accountOwner);
        }

        @Test
        @DisplayName("unieważnia oczekujące zaproszenia PRZED usunięciem konta")
        void shouldRevokeInvitationsBeforeDeletingAccount() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(accountOwner));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            profileService.deleteAccount(3L, new DeleteAccountRequest(PASSWORD));

            InOrder order = inOrder(invitationRepository, userRepository);
            order.verify(invitationRepository).revokePendingByInviter(eq(3L), any(LocalDateTime.class));
            order.verify(userRepository).delete(accountOwner);
        }

        @Test
        @DisplayName("odrzuca złe hasło i niczego nie usuwa")
        void shouldRejectWrongPassword() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(accountOwner));
            when(passwordEncoder.matches("ZleHaslo", "$2a$10$zahashowane")).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> profileService.deleteAccount(3L, new DeleteAccountRequest("ZleHaslo")));

            assertTrue(ex.getMessage().contains("Nieprawidłowe hasło"));

            verify(userRepository, never()).delete(any(User.class));
            verify(invitationRepository, never()).revokePendingByInviter(anyLong(), any());
        }

        @Test
        @DisplayName("nie pozwala usunąć konta ostatniego administratora")
        void shouldBlockLastAdmin() {
            accountOwner.setRole(Role.ADMIN);

            when(userRepository.findById(3L)).thenReturn(Optional.of(accountOwner));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

            assertThrows(LastAdminException.class,
                    () -> profileService.deleteAccount(3L, new DeleteAccountRequest(PASSWORD)));

            verify(userRepository, never()).delete(any(User.class));
            verify(invitationRepository, never()).revokePendingByInviter(anyLong(), any());
        }

        @Test
        @DisplayName("pozwala usunąć administratora, gdy jest jeszcze inny")
        void shouldAllowAdminDeletionWhenAnotherAdminExists() {
            accountOwner.setRole(Role.ADMIN);

            when(userRepository.findById(3L)).thenReturn(Optional.of(accountOwner));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);

            profileService.deleteAccount(3L, new DeleteAccountRequest(PASSWORD));

            verify(userRepository).delete(accountOwner);
        }

        @Test
        @DisplayName("nie liczy administratorów przy usuwaniu zwykłego konta")
        void shouldNotCountAdminsForRegularUser() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(accountOwner));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            profileService.deleteAccount(3L, new DeleteAccountRequest(PASSWORD));

            verify(userRepository, never()).countByRole(any(Role.class));
        }

        @Test
        @DisplayName("sprawdza hasło zanim sięgnie po liczbę administratorów")
        void shouldVerifyPasswordBeforeCheckingAdminCount() {
            accountOwner.setRole(Role.ADMIN);

            when(userRepository.findById(3L)).thenReturn(Optional.of(accountOwner));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThrows(IllegalArgumentException.class,
                    () -> profileService.deleteAccount(3L, new DeleteAccountRequest("ZleHaslo")));

            verify(userRepository, never()).countByRole(any(Role.class));
        }

        @Test
        @DisplayName("rzuca wyjątek dla nieistniejącego konta")
        void shouldThrowWhenAccountDoesNotExist() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> profileService.deleteAccount(99L, new DeleteAccountRequest(PASSWORD)));

            verify(userRepository, never()).delete(any(User.class));
        }
    }
}
