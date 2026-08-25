package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationResponse;

import java.util.List;

public interface InvitationService {

    InvitationResponse createInvitation(InvitationRequest request, Long invitedById);

    List<InvitationResponse> findAllInvitations();

    void revokeInvitation(Long id);
}
