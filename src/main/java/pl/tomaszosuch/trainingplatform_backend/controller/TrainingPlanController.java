package pl.tomaszosuch.trainingplatform_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.StatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.TrainingPlanRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.TrainingPlanService;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/training-plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    @GetMapping
    public ResponseEntity<List<TrainingPlanResponse>> getUserPlans(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                trainingPlanService.getTrainingPlansByUserId(currentUser.getId(), from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingPlanResponse> getById(@AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(trainingPlanService.getTrainingPlanById(id, currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<TrainingPlanResponse> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TrainingPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingPlanService.createTrainingPlan(currentUser.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingPlanResponse> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody TrainingPlanRequest request) {
        return ResponseEntity.ok(
                trainingPlanService.updateTrainingPlan(currentUser.getId(), id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TrainingPlanResponse> changeStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(
                trainingPlanService.changeStatus(currentUser.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        trainingPlanService.deleteTrainingPlan(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

}
