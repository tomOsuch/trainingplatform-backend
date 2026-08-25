package pl.tomaszosuch.trainingplatform_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.InvitationService;

import java.util.List;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponse> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody InvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.createInvitation(request, currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> getAll() {
        return ResponseEntity.ok(invitationService.findAllInvitations());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        invitationService.revokeInvitation(id);
        return ResponseEntity.noContent().build();
    }
}
