package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import pl.tomaszosuch.trainingplatform_backend.config.InvitationProperties;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Invitation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailAlreadyRegisteredException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvitationAlreadyResolvedException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvitationNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.InvitationMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.InvitationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.security.RateLimiter;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.service.EmailService;
import pl.tomaszosuch.trainingplatform_backend.service.InvitationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final SecureTokenGenerator tokenGenerator;
    private final InvitationMapper invitationMapper;
    private final EmailService emailService;
    private final InvitationProperties properties;
    private final RateLimiter rateLimiter;

    @Override
    public InvitationResponse createInvitation(InvitationRequest request, Long invitedById) {

        rateLimiter.checkInvitationCreation(invitedById);

        String email = request.email().trim();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        User inviter = userRepository.findById(invitedById)
            .orElseThrow(() -> new UserNotFoundException(invitedById));

        revokePrevious(email);

        String token = tokenGenerator.generateToken();

        Invitation invitation = invitationRepository.save(Invitation.builder()
                .email(email)
                .tokenHash(tokenGenerator.hash(token))
                .role(request.roleOrDefault())
                .invitedBy(inviter)
                .expiresAt(LocalDateTime.now().plusDays(properties.getExpirationDays()))
                .build());

        deliver(invitation, token);

        return invitationMapper.toResponse(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> findAllInvitations() {
        return invitationRepository.findAllWithInviter().stream()
                .map(invitationMapper::toResponse)
                .toList();
    }

    @Override
    public void revokeInvitation(Long id) {

        Invitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new InvitationNotFoundException(id));

        if (invitation.getUsedAt() != null) {
            throw new InvitationAlreadyResolvedException("Zaproszenie zostało już wykorzystane i nie można go unieważnić");
        }

        if (invitation.getRevokedAt() != null) {
            throw new InvitationAlreadyResolvedException("Zaproszenie jest już unieważnione");
        }

        invitation.setRevokedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

    }

    private void revokePrevious(String email) {
        invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(email)
                .ifPresent(previous -> {
                    previous.setRevokedAt(LocalDateTime.now());
                    invitationRepository.saveAndFlush(previous);
                    log.info("Unieważniono poprzednie zaproszenie (id={}) dla adresu {}",
                            previous.getId(), email);
                });
    }

    private void deliver(Invitation invitation, String token) {
        try {
            emailService.sendInvitation(
                    invitation.getEmail(),
                    buildAcceptUrl(token),
                    invitation.getExpiresAt());

            invitation.setSentAt(LocalDateTime.now());

        } catch (RuntimeException ex) {
            log.error("Nie udało się wysłać zaproszenia (id={}) na adres {}: {}",
                    invitation.getId(), invitation.getEmail(), ex.getMessage(), ex);
        }
    }

    private String buildAcceptUrl(String token) {
        return UriComponentsBuilder.fromUriString(properties.getAcceptBaseUrl())
                .queryParam("token", token)
                .toUriString();
    }
}
