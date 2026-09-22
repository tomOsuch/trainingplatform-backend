package pl.tomaszosuch.trainingplatform_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.tomaszosuch.trainingplatform_backend.dto.request.CooperationInviteRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationDecisionRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationInvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.CooperationService;

import java.util.List;

@RestController
@RequestMapping("/cooperation-invitations")
@RequiredArgsConstructor
public class CooperationInvitationController {

    private final CooperationService cooperationService;

    @PostMapping
    public ResponseEntity<CooperationInvitationResponse> invite(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CooperationInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cooperationService.invite(currentUser.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<CooperationInvitationResponse>> pending(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(cooperationService.pendingInvitations(currentUser.getId()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CooperationInvitationResponse> respond(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody InvitationDecisionRequest request) {
        return ResponseEntity.ok(cooperationService.respond(currentUser.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        cooperationService.withdraw(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
