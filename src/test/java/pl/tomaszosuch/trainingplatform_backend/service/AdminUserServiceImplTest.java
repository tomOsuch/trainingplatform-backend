package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.tomaszosuch.trainingplatform_backend.dto.response.AdminUserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.AccountStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.LastAdminException;
import pl.tomaszosuch.trainingplatform_backend.exception.SelfDeactivationException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapperImpl;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.AdminUserServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserServiceImplTest")
class AdminUserServiceImplTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Spy
    private UserMapper userMapper = new UserMapperImpl();

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private static User account(Long id, Role role, boolean active) {
        return User.builder()
                .id(id).email("konto" + id + "@example.com").password("hash")
                .firstName("Jan").lastName("Testowy")
                .role(role).isActive(active)
                .build();
    }

    private void stubFound(User user) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("wyłączenie konta zmienia flagę i unieważnia sesje")
    void shouldDeactivateAndRevokeSessions() {
        User target = account(5L, Role.USER, true);
        stubFound(target);

        AdminUserResponse response = adminUserService.changeStatus(ADMIN_ID, 5L, AccountStatus.INACTIVE);

        assertFalse(response.active());
        assertFalse(target.getIsActive());
        verify(userRepository).save(target);
        verify(refreshTokenService).revokeAllForUser(5L);
        verify(userRepository, never()).countActiveByRole(any());
    }

    @Test
    @DisplayName("przywrócenie konta zmienia flagę i nie dotyka sesji")
    void shouldRestoreWithoutTouchingSessions() {
        User target = account(5L, Role.USER, false);
        stubFound(target);

        AdminUserResponse response = adminUserService.changeStatus(ADMIN_ID, 5L, AccountStatus.ACTIVE);

        assertTrue(response.active());
        assertTrue(target.getIsActive());
        verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    }

    @Test
    @DisplayName("nie pozwala wyłączyć własnego konta — z własnym komunikatem")
    void shouldRejectSelfDeactivation() {
        stubFound(account(ADMIN_ID, Role.ADMIN, true));

        SelfDeactivationException ex = assertThrows(SelfDeactivationException.class,
                () -> adminUserService.changeStatus(ADMIN_ID, ADMIN_ID, AccountStatus.INACTIVE));

        assertEquals("Nie możesz wyłączyć własnego konta", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    }

    @Test
    @DisplayName("nie pozwala wyłączyć ostatniego aktywnego administratora — z własnym komunikatem")
    void shouldRejectLastActiveAdmin() {
        stubFound(account(3L, Role.ADMIN, true));
        when(userRepository.countActiveByRole(Role.ADMIN)).thenReturn(1L);

        LastAdminException ex = assertThrows(LastAdminException.class,
                () -> adminUserService.changeStatus(ADMIN_ID, 3L, AccountStatus.INACTIVE));

        assertEquals("To ostatnie aktywne konto administratora — po jego wyłączeniu nikt nie odzyska dostępu do panelu",
                ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenService, never()).revokeAllForUser(anyLong());
    }

    @Test
    @DisplayName("pozwala wyłączyć administratora, gdy zostaje inny aktywny")
    void shouldDeactivateAdminWhenAnotherActiveRemains() {
        User target = account(3L, Role.ADMIN, true);
        stubFound(target);
        when(userRepository.countActiveByRole(Role.ADMIN)).thenReturn(2L);

        adminUserService.changeStatus(ADMIN_ID, 3L, AccountStatus.INACTIVE);

        assertFalse(target.getIsActive());
        verify(refreshTokenService).revokeAllForUser(3L);
    }

    @Test
    @DisplayName("ponowne wyłączenie wyłączonego administratora nie sprawdza licznika")
    void shouldNotCountAdminsWhenTargetAlreadyInactive() {
        stubFound(account(3L, Role.ADMIN, false));

        adminUserService.changeStatus(ADMIN_ID, 3L, AccountStatus.INACTIVE);

        verify(userRepository, never()).countActiveByRole(any());
    }

    @Test
    @DisplayName("nieistniejące konto daje UserNotFoundException")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> adminUserService.changeStatus(ADMIN_ID, 99L, AccountStatus.INACTIVE));
    }
}