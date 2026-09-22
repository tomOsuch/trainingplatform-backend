package pl.tomaszosuch.trainingplatform_backend.service;

import java.util.List;

import pl.tomaszosuch.trainingplatform_backend.dto.request.CooperationInviteRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationDecisionRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationInvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationResponse;

public interface CooperationService {

    CooperationInvitationResponse invite(Long coachId, CooperationInviteRequest request);

    List<CooperationInvitationResponse> pendingInvitations(Long userId);

    CooperationInvitationResponse respond(Long athleteId, Long invitationId, InvitationDecisionRequest request);

    List<CooperationResponse> activeCooperations(Long userId);

    void end(Long userId, Long cooperationId);

    void withdraw(Long coachId, Long invitationId);
}