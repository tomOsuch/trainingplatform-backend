package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.dto.response.AdminUserResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PageResponse;
import pl.tomaszosuch.trainingplatform_backend.enums.AccountStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.AdminUserSort;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.AdminUserService;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> findUsers(String search, AccountStatus status,
                                                     AdminUserSort sort, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, toSort(sort));
        return PageResponse.from(
                userRepository.findForAdmin(toPattern(search), toActiveStates(status), pageRequest)
                        .map(userMapper::toAdminResponse));
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
