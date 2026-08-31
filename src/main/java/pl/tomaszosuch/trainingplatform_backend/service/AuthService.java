package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.LoginResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResult login(LoginRequest request, String userAgent);

    LoginResult refresh(String rawRefreshToken, String userAgent);

    void logout(String rawRefreshToken);

    InvitationCheckResponse checkInvitation(String token);

    record LoginResult(LoginResponse response, RefreshTokenService.IssuedToken refreshToken) {}
}