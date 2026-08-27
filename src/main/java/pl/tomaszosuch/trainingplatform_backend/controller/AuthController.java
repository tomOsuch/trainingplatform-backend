package pl.tomaszosuch.trainingplatform_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pl.tomaszosuch.trainingplatform_backend.dto.request.LoginRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetConfirmRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.PasswordResetRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.RegisterRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PasswordResetCheckResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.service.AuthService;
import pl.tomaszosuch.trainingplatform_backend.service.PasswordResetService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/invitation")
    public ResponseEntity<InvitationCheckResponse> checkInvitation(@RequestParam String token) {
        return ResponseEntity.ok(authService.checkInvitation(token));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/password-reset")
    public ResponseEntity<PasswordResetCheckResponse> checkPasswordResetToken(@RequestParam String token) {
        return ResponseEntity.ok(passwordResetService.checkToken(token));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request);
        return ResponseEntity.noContent().build();
    }
}