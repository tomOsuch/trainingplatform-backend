package pl.tomaszosuch.trainingplatform_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalStatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalDetailsResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;
import pl.tomaszosuch.trainingplatform_backend.service.GoalService;

import java.util.List;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<GoalResponse>> getGoals(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String status) {
        GoalStatus filter = status == null ? null : GoalStatus.fromParam(status);
        return ResponseEntity.ok(goalService.getGoals(currentUser.getId(), filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalDetailsResponse> getById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(goalService.getGoal(currentUser.getId(), id));
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.createGoal(currentUser.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.ok(goalService.updateGoal(currentUser.getId(), id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<GoalResponse> changeStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody GoalStatusUpdateRequest request) {
        return ResponseEntity.ok(goalService.changeStatus(currentUser.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        goalService.deleteGoal(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
