package pl.tomaszosuch.trainingplatform_backend.controller;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.*;
import pl.tomaszosuch.trainingplatform_backend.dto.response.NotificationPreferencesResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.UserResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.ProfileService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                profileService.getProfile(currentUser.getId()));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                profileService.updateProfile(currentUser.getId(), request));
    }

    @GetMapping("/notifications")
    public ResponseEntity<NotificationPreferencesResponse> getNotificationPreferences(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                profileService.getNotificationPreferences(currentUser.getId()));
    }

    @PutMapping("/notifications")
    public ResponseEntity<NotificationPreferencesResponse> updateNotificationPreferences(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody NotificationPreferencesRequest request) {
        return ResponseEntity.ok(
                profileService.updateNotificationPreferences(currentUser.getId(), request));
    }

    @PutMapping("/coach")
    public ResponseEntity<UserResponse> setCoachMode(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CoachModeRequest request) {
        return ResponseEntity.ok(
                profileService.setCoachMode(currentUser.getId(), request.enabled()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DeleteAccountRequest request) {
        profileService.deleteAccount(currentUser.getId(), request);
        return ResponseEntity.noContent().build();
    }

}
