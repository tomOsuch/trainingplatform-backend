package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.response.AdminUserResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PageResponse;
import pl.tomaszosuch.trainingplatform_backend.enums.AccountStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.AdminUserSort;

public interface AdminUserService {

    PageResponse<AdminUserResponse> findUsers(String search, AccountStatus status,
                                              AdminUserSort sort, int page, int size);

    AdminUserResponse changeStatus(Long adminId, Long userId, AccountStatus status);
}