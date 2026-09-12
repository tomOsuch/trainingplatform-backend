package pl.tomaszosuch.trainingplatform_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.AccountStatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.AdminUserResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PageResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.AccountStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.AdminUserSort;
import pl.tomaszosuch.trainingplatform_backend.service.AdminUserService;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;

    @GetMapping("/users")
    public ResponseEntity<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) @Size(max = 100) String search,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "LAST_NAME") AdminUserSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(adminUserService.findUsers(search, status, sort, page, size));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponse> changeUserStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody AccountStatusUpdateRequest request) {
        return ResponseEntity.ok(adminUserService.changeStatus(currentUser.getId(), id, request.status()));
    }
}