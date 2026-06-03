package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.ChangePasswordRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.UpdateProfileRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;

public interface UserService {

    UserResponse getUserProfile(Long id);
    UserResponse updateUserProfile(Long id, UpdateProfileRequest request);
    void changePassword(Long id, ChangePasswordRequest request);
}
