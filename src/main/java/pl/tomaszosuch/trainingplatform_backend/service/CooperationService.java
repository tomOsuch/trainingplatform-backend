package pl.tomaszosuch.trainingplatform_backend.service;

import java.util.List;

import pl.tomaszosuch.trainingplatform_backend.dto.request.CooperationInviteRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationDecisionRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationInvitationResponse;

public interface CooperationService {

    CooperationInvitationResponse invite(Long coachId, CooperationInviteRequest request);

    List<CooperationInvitationResponse> receivedInvitations(Long athleteId);

    CooperationInvitationResponse respond(Long athleteId, Long invitationId, InvitationDecisionRequest request);
}