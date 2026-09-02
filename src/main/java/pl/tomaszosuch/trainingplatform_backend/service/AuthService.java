package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.LoginResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.security.ClientInfo;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResult login(LoginRequest request, ClientInfo clientInfo);

    LoginResult refresh(String rawRefreshToken, ClientInfo clientInfo);

    void logout(String rawRefreshToken);

    InvitationCheckResponse checkInvitation(String token);

    record LoginResult(LoginResponse response, RefreshTokenService.IssuedToken refreshToken) {}
}