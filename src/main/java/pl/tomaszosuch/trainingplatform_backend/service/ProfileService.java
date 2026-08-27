package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.ChangePasswordRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.DeleteAccountRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.UpdateProfileRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;

public interface ProfileService {

    UserResponse getProfile(Long id);
    UserResponse updateProfile(Long id, UpdateProfileRequest request);
    void changePassword(Long id, ChangePasswordRequest request);
    void deleteAccount(Long userId, DeleteAccountRequest request);
}
