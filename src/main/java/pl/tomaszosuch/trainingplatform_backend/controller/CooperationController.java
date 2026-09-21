package pl.tomaszosuch.trainingplatform_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.CooperationService;

import java.util.List;

@RestController
@RequestMapping("/cooperations")
@RequiredArgsConstructor
public class CooperationController {

    private final CooperationService cooperationService;

    @GetMapping
    public ResponseEntity<List<CooperationResponse>> getActive(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(cooperationService.activeCooperations(currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> end(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        cooperationService.end(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

}
