package pl.tomaszosuch.trainingplatform_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutTemplateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutTemplateResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.WorkoutTemplateService;

import java.util.List;

@RestController
@RequestMapping("/workout-templates")
@RequiredArgsConstructor
public class WorkoutTemplateController {

    private final WorkoutTemplateService workoutTemplateService;

    @GetMapping
    public ResponseEntity<List<WorkoutTemplateResponse>> getTemplates(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(workoutTemplateService.getTemplates(currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkoutTemplateResponse> getById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(workoutTemplateService.getTemplate(currentUser.getId(), id));
    }

    @PostMapping
    public ResponseEntity<WorkoutTemplateResponse> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody WorkoutTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workoutTemplateService.createTemplate(currentUser.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkoutTemplateResponse> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody WorkoutTemplateRequest request) {
        return ResponseEntity.ok(workoutTemplateService.updateTemplate(currentUser.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        workoutTemplateService.deleteTemplate(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
