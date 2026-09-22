package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.config.CooperationProperties;
import pl.tomaszosuch.trainingplatform_backend.dto.request.CooperationInviteRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationDecisionRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationInvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationRole;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.InvitationDecision;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationConflictException;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.repository.CooperationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.security.RateLimiter;
import pl.tomaszosuch.trainingplatform_backend.service.CooperationService;

import org.springframework.security.access.AccessDeniedException;
import pl.tomaszosuch.trainingplatform_backend.service.EmailService;

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
    private static final String NOT_PARTICIPANT_MESSAGE = "Nie jesteś stroną tej współpracy";
    private static final String NOT_ACTIVE_MESSAGE = "Ta współpraca nie jest aktywna";
    private static final String NOT_SENDER_MESSAGE = "To nie jest Twoje zaproszenie";
    private static final String TOO_MANY_PENDING_MESSAGE = "Masz już %d oczekujących zaproszeń — wycofaj któreś albo poczekaj na odpowiedzi";

    private final CooperationRepository cooperationRepository;
    private final UserRepository userRepository;
    private final CooperationProperties properties;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;

    @Override
    public CooperationInvitationResponse invite(Long coachId, CooperationInviteRequest request) {

        rateLimiter.checkCooperationInvitation(coachId);

        String email = request.email().trim();

        User athlete = userRepository.findByEmail(email)
                .filter(User::getIsActive)
                .orElseThrow(() -> new UserNotFoundException(email));

        rateLimiter.refundCooperationInvitationMiss(coachId);

        if (athlete.getId().equals(coachId)) {
            throw new IllegalArgumentException(SELF_INVITE_MESSAGE);
        }

        User coach = userRepository.lockById(coachId)
                .orElseThrow(() -> new UserNotFoundException(coachId));

        requirePairIsFree(coachId, athlete.getId());
        requireRoomForAnotherInvitation(coachId);

        Cooperation invitation = cooperationRepository.save(Cooperation.builder()
                .coach(coach)
                .athlete(athlete)
                .status(CooperationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(properties.getInvitationExpirationDays()))
                .build());

        log.info("Użytkownik {} zaprosił do współpracy {} (zaproszenie id={})",
                coachId, athlete.getId(), invitation.getId());

        deliver(invitation);

        return toInvitationResponse(invitation, coachId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CooperationInvitationResponse> pendingInvitations(Long userId) {
        return cooperationRepository
                .findByParticipantAndStatus(userId, CooperationStatus.PENDING).stream()
                .filter(invitation -> !hasExpired(invitation))
                .map(invitation -> toInvitationResponse(invitation, userId))
                .toList();
    }

    @Override
    public CooperationInvitationResponse respond(Long athleteId, Long invitationId, InvitationDecisionRequest request) {

        Cooperation invitation = cooperationRepository.lockById(invitationId)
                .orElseThrow(() -> new CooperationNotFoundException(invitationId));

        if (!invitation.getAthlete().getId().equals(athleteId)) {
            throw new AccessDeniedException(NOT_ADDRESSEE_MESSAGE);
        }

        if (invitation.getStatus() != CooperationStatus.PENDING) {
            throw new CooperationConflictException(ALREADY_RESOLVED_MESSAGE);
        }

        if (hasExpired(invitation)) {
            throw new CooperationConflictException(EXPIRED_MESSAGE);
        }

        invitation.setStatus(request.decision() == InvitationDecision.ACCEPTED
                ? CooperationStatus.ACTIVE
                : CooperationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());

        log.info("Użytkownik {} odpowiedział na zaproszenie id={}: {}",
                athleteId, invitationId, invitation.getStatus());

        return toInvitationResponse(cooperationRepository.save(invitation), athleteId);
    }

    private static CooperationInvitationResponse toInvitationResponse(Cooperation invitation, Long userId) {
        boolean askingAsCoach = invitation.getCoach().getId().equals(userId);
        User partner = askingAsCoach ? invitation.getAthlete() : invitation.getCoach();

        return new CooperationInvitationResponse(
                invitation.getId(),
                askingAsCoach ? CooperationRole.COACH : CooperationRole.ATHLETE,
                partner.getId(),
                partner.getFirstName(),
                partner.getLastName(),
                partner.getEmail(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CooperationResponse> activeCooperations(Long userId) {
        return cooperationRepository
                .findByParticipantAndStatus(userId, CooperationStatus.ACTIVE).stream()
                .map(cooperation -> toResponse(cooperation, userId))
                .toList();
    }

    @Override
    public void end(Long userId, Long cooperationId) {
        Cooperation cooperation = cooperationRepository.lockById(cooperationId)
                .orElseThrow(() -> new CooperationNotFoundException(cooperationId));

        boolean isCoach = cooperation.getCoach().getId().equals(userId);
        boolean isAthlete = cooperation.getAthlete().getId().equals(userId);

        if (!isCoach && !isAthlete) {
            throw new AccessDeniedException(NOT_PARTICIPANT_MESSAGE);
        }

        if (cooperation.getStatus() != CooperationStatus.ACTIVE) {
            throw new CooperationConflictException(NOT_ACTIVE_MESSAGE);
        }

        cooperation.setStatus(CooperationStatus.ENDED);
        cooperation.setEndedAt(LocalDateTime.now());
        cooperationRepository.save(cooperation);

        log.info("Użytkownik {} zakończył współpracę id={}", userId, cooperationId);

        notifyOtherParty(cooperation, isCoach);
    }

    @Override
    public void withdraw(Long coachId, Long invitationId) {

        Cooperation invitation = cooperationRepository.lockById(invitationId)
                .orElseThrow(() -> new CooperationNotFoundException(invitationId));

        if (!invitation.getCoach().getId().equals(coachId)) {
            throw new AccessDeniedException(NOT_SENDER_MESSAGE);
        }

        if (invitation.getStatus() != CooperationStatus.PENDING) {
            throw new CooperationConflictException(ALREADY_RESOLVED_MESSAGE);
        }

        invitation.setStatus(CooperationStatus.WITHDRAWN);
        invitation.setEndedAt(LocalDateTime.now());
        cooperationRepository.save(invitation);

        log.info("Użytkownik {} wycofał zaproszenie id={}", coachId, invitationId);
    }

    private static CooperationResponse toResponse(Cooperation cooperation, Long userId) {
        boolean askingAsCoach = cooperation.getCoach().getId().equals(userId);
        User partner = askingAsCoach ? cooperation.getAthlete() : cooperation.getCoach();

        return new CooperationResponse(
                cooperation.getId(),
                askingAsCoach ? CooperationRole.COACH : CooperationRole.ATHLETE,
                partner.getId(),
                partner.getFirstName(),
                partner.getLastName(),
                partner.getEmail(),
                cooperation.getRespondedAt());
    }

    private void notifyOtherParty(Cooperation cooperation, boolean endedByCoach) {
        User initiator = endedByCoach ? cooperation.getCoach() : cooperation.getAthlete();
        User recipient = endedByCoach ? cooperation.getAthlete() : cooperation.getCoach();

        try {
            emailService.sendCooperationEnded(recipient.getEmail(), fullName(initiator));

        } catch (RuntimeException ex) {
            log.error("Nie udało się powiadomić o zakończeniu współpracy (id={}): {}",
                    cooperation.getId(), ex.getMessage(), ex);
        }
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

    private void requireRoomForAnotherInvitation(Long coachId) {
        long pending = cooperationRepository.countByCoachIdAndStatusAndExpiresAtAfter(
                coachId, CooperationStatus.PENDING, LocalDateTime.now());

        if (pending >= properties.getMaxPendingInvitations()) {
            throw new CooperationConflictException(TOO_MANY_PENDING_MESSAGE.formatted(pending));
        }
    }

    private void expire(Cooperation invitation) {
        invitation.setStatus(CooperationStatus.EXPIRED);
        cooperationRepository.saveAndFlush(invitation);

        log.info("Zaproszenie id={} wygasło i zostało domknięte", invitation.getId());
    }

    private void deliver(Cooperation invitation) {
        try {
            emailService.sendCooperationInvitation(
                    invitation.getAthlete().getEmail(),
                    fullName(invitation.getCoach()),
                    properties.getInvitationsUrl(),
                    invitation.getExpiresAt());

        } catch (RuntimeException ex) {
            log.error("Nie udało się wysłać zaproszenia do współpracy (id={}) na adres {}: {}",
                    invitation.getId(), invitation.getAthlete().getEmail(), ex.getMessage(), ex);
        }
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private static boolean hasExpired(Cooperation invitation) {
        return invitation.getExpiresAt() != null
                && invitation.getExpiresAt().isBefore(LocalDateTime.now());
    }
}
