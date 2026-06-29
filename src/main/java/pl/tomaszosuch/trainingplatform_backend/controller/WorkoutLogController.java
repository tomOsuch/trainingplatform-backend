package pl.tomaszosuch.trainingplatform_backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutLogRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutLogResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.WorkoutLogService;

@RestController
@RequestMapping("/workout-logs")
@RequiredArgsConstructor
public class WorkoutLogController {

    private final WorkoutLogService logService;

    @GetMapping
    public ResponseEntity<List<WorkoutLogResponse>> getUserLogs(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
            logService.getUserLogs(currentUser.getId(), categoryId, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkoutLogResponse> getById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(
            logService.getById(currentUser.getId(), id));
    }

    @PostMapping
    public ResponseEntity<WorkoutLogResponse> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody WorkoutLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(logService.create(currentUser.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkoutLogResponse> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody WorkoutLogRequest request) {
        return ResponseEntity.ok(
            logService.update(currentUser.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        logService.delete(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

}
