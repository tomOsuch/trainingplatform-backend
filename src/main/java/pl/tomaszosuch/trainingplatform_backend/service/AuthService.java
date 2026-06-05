package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.LoginResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
