package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetConfirmRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PasswordResetCheckResponse;

public interface PasswordResetService {

    void requestReset(PasswordResetRequest request);

    PasswordResetCheckResponse checkToken(String token);

    void confirmReset(PasswordResetConfirmRequest request);
}
