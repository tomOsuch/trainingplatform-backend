package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetConfirmRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PasswordResetCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.security.ClientInfo;

public interface PasswordResetService {

    void requestReset(PasswordResetRequest request, ClientInfo clientInfo);

    PasswordResetCheckResponse checkToken(String token);

    void confirmReset(PasswordResetConfirmRequest request);
}
