package pl.tomaszosuch.trainingplatform_backend.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.tomaszosuch.trainingplatform_backend.dto.response.AdminUserResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PageResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.AccountStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.AdminUserSort;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.LastAdminException;
import pl.tomaszosuch.trainingplatform_backend.exception.SelfDeactivationException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.AdminUserService;
import pl.tomaszosuch.trainingplatform_backend.service.RefreshTokenService;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    static final String LAST_ACTIVE_ADMIN_MESSAGE =
            "To ostatnie aktywne konto administratora — po jego wyłączeniu nikt nie odzyska dostępu do panelu";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> findUsers(String search, AccountStatus status,
                                                     AdminUserSort sort, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, toSort(sort));
        return PageResponse.from(
                userRepository.findForAdmin(toPattern(search), toActiveStates(status), pageRequest)
                        .map(userMapper::toAdminResponse));
    }

    @Override
    public AdminUserResponse changeStatus(Long adminId, Long userId, AccountStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        switch (status) {
            case INACTIVE -> deactivate(adminId, user);
            case ACTIVE -> activate(adminId, user);
        }
        return userMapper.toAdminResponse(user);
    }

    private void deactivate(Long adminId, User user) {
        if (user.getId().equals(adminId)) {
            throw new SelfDeactivationException();
        }
        if (user.getRole() == Role.ADMIN
                && Boolean.TRUE.equals(user.getIsActive())
                && userRepository.countActiveByRole(Role.ADMIN) <= 1) {
            throw new LastAdminException(LAST_ACTIVE_ADMIN_MESSAGE);
        }

        user.setIsActive(false);
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());

        log.warn("Administrator {} wyłączył konto {} (rola {})", adminId, user.getEmail(), user.getRole());
    }

    private void activate(Long adminId, User user) {
        user.setIsActive(true);
        userRepository.save(user);

        log.info("Administrator {} przywrócił konto {}", adminId, user.getEmail());
    }

    private static Sort toSort(AdminUserSort sort) {
        return switch (sort) {
            case LAST_NAME -> Sort.by("lastName", "firstName", "id");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt", "id");
        };
    }

    private static String toPattern(String search) {
        if (search == null || search.isBlank()) {
            return "%";
        }
        String escaped = search.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private static List<Boolean> toActiveStates(AccountStatus status) {
        if (status == null) {
            return List.of(true, false);
        }
        return switch (status) {
            case ACTIVE -> List.of(true);
            case INACTIVE -> List.of(false);
        };
    }
}