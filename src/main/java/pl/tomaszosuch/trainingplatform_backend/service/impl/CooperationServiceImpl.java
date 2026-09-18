package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.config.CooperationProperties;
import pl.tomaszosuch.trainingplatform_backend.dto.request.CooperationInviteRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationDecisionRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationInvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.InvitationDecision;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationConflictException;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.CooperationMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.CooperationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.CooperationService;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CooperationServiceImpl implements CooperationService {

    private static final List<CooperationStatus> OPEN_STATUSES =
            List.of(CooperationStatus.PENDING, CooperationStatus.ACTIVE);

    private static final String SELF_INVITE_MESSAGE = "Nie możesz zaprosić samego siebie do współpracy";
    private static final String ALREADY_PENDING_MESSAGE = "Zaproszenie dla tej osoby już czeka na odpowiedź";
    private static final String ALREADY_ACTIVE_MESSAGE = "Prowadzisz już tę osobę";
    private static final String ALREADY_RESOLVED_MESSAGE = "To zaproszenie zostało już rozstrzygnięte";
    private static final String EXPIRED_MESSAGE = "Zaproszenie straciło ważność";
    private static final String NOT_ADDRESSEE_MESSAGE = "To zaproszenie nie jest skierowane do Ciebie";

    private final CooperationRepository cooperationRepository;
    private final UserRepository userRepository;
    private final CooperationMapper cooperationMapper;
    private final CooperationProperties properties;

    @Override
    public CooperationInvitationResponse invite(Long coachId, CooperationInviteRequest request) {

        String email = request.email().trim();

        User athlete = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (athlete.getId().equals(coachId)) {
            throw new IllegalArgumentException(SELF_INVITE_MESSAGE);
        }

        requirePairIsFree(coachId, athlete.getId());

        User coach = userRepository.findById(coachId)
                .orElseThrow(() -> new UserNotFoundException(coachId));

        Cooperation invitation = cooperationRepository.save(Cooperation.builder()
                .coach(coach)
                .athlete(athlete)
                .status(CooperationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(properties.getInvitationExpirationDays()))
                .build());

        log.info("Użytkownik {} zaprosił do współpracy {} (zaproszenie id={})",
                coachId, athlete.getId(), invitation.getId());

        return cooperationMapper.toResponse(invitation);
    }

    @Override
    public List<CooperationInvitationResponse> receivedInvitations(Long athleteId) {
        return cooperationRepository.findByAthleteIdAndStatus(athleteId, CooperationStatus.PENDING).stream()
                .filter(invitation -> !hasExpired(invitation))
                .map(cooperationMapper::toResponse)
                .toList();
    }

    @Override
    public CooperationInvitationResponse respond(Long athleteId, Long invitationId, InvitationDecisionRequest request) {

        Cooperation invitation = cooperationRepository.findById(invitationId)
                .orElseThrow(() -> new CooperationNotFoundException(invitationId));

        if (!invitation.getAthlete().getId().equals(athleteId)) {
            throw new AccessDeniedException(NOT_ADDRESSEE_MESSAGE);
        }

        if (invitation.getStatus() != CooperationStatus.PENDING) {
            throw new CooperationConflictException(ALREADY_RESOLVED_MESSAGE);
        }

        if (hasExpired(invitation)) {
            expire(invitation);
            throw new CooperationConflictException(EXPIRED_MESSAGE);
        }

        invitation.setStatus(request.decision() == InvitationDecision.ACCEPTED
                ? CooperationStatus.ACTIVE
                : CooperationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());

        log.info("Użytkownik {} odpowiedział na zaproszenie id={}: {}",
                athleteId, invitationId, invitation.getStatus());

        return cooperationMapper.toResponse(cooperationRepository.save(invitation));
    }

    private void requirePairIsFree(Long coachId, Long athleteId) {
        Optional<Cooperation> open = cooperationRepository
                .findByCoachIdAndAthleteIdAndStatusIn(coachId, athleteId, OPEN_STATUSES);

        if (open.isEmpty()) {
            return;
        }

        Cooperation existing = open.get();

        if (existing.getStatus() == CooperationStatus.ACTIVE) {
            throw new CooperationConflictException(ALREADY_ACTIVE_MESSAGE);
        }

        if (!hasExpired(existing)) {
            throw new CooperationConflictException(ALREADY_PENDING_MESSAGE);
        }

        expire(existing);
    }

    private void expire(Cooperation invitation) {
        invitation.setStatus(CooperationStatus.EXPIRED);
        cooperationRepository.saveAndFlush(invitation);

        log.info("Zaproszenie id={} wygasło i zostało domknięte", invitation.getId());
    }

    private static boolean hasExpired(Cooperation invitation) {
        return invitation.getExpiresAt() != null
                && invitation.getExpiresAt().isBefore(LocalDateTime.now());
    }
}
