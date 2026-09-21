package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import pl.tomaszosuch.trainingplatform_backend.config.CooperationProperties;
import pl.tomaszosuch.trainingplatform_backend.dto.request.CooperationInviteRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationDecisionRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.CooperationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationRole;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.InvitationDecision;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationConflictException;
import pl.tomaszosuch.trainingplatform_backend.exception.CooperationNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.CooperationMapper;
import pl.tomaszosuch.trainingplatform_backend.mapper.CooperationMapperImpl;
import pl.tomaszosuch.trainingplatform_backend.repository.CooperationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.CooperationServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("CooperationServiceImplTest")
class CooperationServiceImplTest {

    private static final Long COACH_ID = 1L;
    private static final Long ATHLETE_ID = 2L;
    private static final String ATHLETE_EMAIL = "podopieczny@example.com";

    @Mock
    private CooperationRepository cooperationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private CooperationServiceImpl service;

    private User coach;
    private User athlete;

    @BeforeEach
    void setUp() {
        coach = user(COACH_ID, "trener@example.com");
        athlete = user(ATHLETE_ID, ATHLETE_EMAIL);

        CooperationProperties properties = new CooperationProperties();
        properties.setInvitationExpirationDays(14);
        properties.setInvitationsUrl("http://localhost:3000/wspolpraca/zaproszenia");

        CooperationMapper mapper = new CooperationMapperImpl();
        service = new CooperationServiceImpl(cooperationRepository, userRepository, mapper,
                properties, emailService);
    }

    private static User user(Long id, String email) {
        return User.builder()
                .id(id).email(email).password("hash")
                .firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private Cooperation invitation(CooperationStatus status, LocalDateTime expiresAt) {
        return Cooperation.builder()
                .id(10L).coach(coach).athlete(athlete)
                .status(status).createdAt(LocalDateTime.now()).expiresAt(expiresAt)
                .build();
    }

    private void givenPairIsFree() {
        when(cooperationRepository.findByCoachIdAndAthleteIdAndStatusIn(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("zaproszenie dostaje termin ważności z konfiguracji")
    void shouldSetExpiryFromProperties() {
        when(userRepository.findByEmail(ATHLETE_EMAIL)).thenReturn(Optional.of(athlete));
        when(userRepository.findById(COACH_ID)).thenReturn(Optional.of(coach));
        givenPairIsFree();
        when(cooperationRepository.save(any(Cooperation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.invite(COACH_ID, new CooperationInviteRequest(ATHLETE_EMAIL));

        ArgumentCaptor<Cooperation> captor = ArgumentCaptor.forClass(Cooperation.class);
        verify(cooperationRepository).save(captor.capture());

        Cooperation saved = captor.getValue();
        assertEquals(CooperationStatus.PENDING, saved.getStatus());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusDays(13)));
        assertTrue(saved.getExpiresAt().isBefore(LocalDateTime.now().plusDays(15)));
    }

    @Test
    @DisplayName("nieistniejący adres daje 404, nie ciche zaproszenie")
    void shouldRejectUnknownEmail() {
        when(userRepository.findByEmail(ATHLETE_EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.invite(COACH_ID, new CooperationInviteRequest(ATHLETE_EMAIL)));

        verify(cooperationRepository, never()).save(any());
    }

    @Test
    @DisplayName("konta wyłączonego nie da się zaprosić — tak samo jak nieistniejącego")
    void shouldRejectInactiveRecipient() {
        athlete.setIsActive(false);
        when(userRepository.findByEmail(ATHLETE_EMAIL)).thenReturn(Optional.of(athlete));

        assertThrows(UserNotFoundException.class,
                () -> service.invite(COACH_ID, new CooperationInviteRequest(ATHLETE_EMAIL)));

        verify(cooperationRepository, never()).save(any());
    }

    @Test
    @DisplayName("nie można zaprosić samego siebie")
    void shouldRejectSelfInvite() {
        when(userRepository.findByEmail("trener@example.com")).thenReturn(Optional.of(coach));

        assertThrows(IllegalArgumentException.class,
                () -> service.invite(COACH_ID, new CooperationInviteRequest("trener@example.com")));

        verify(cooperationRepository, never()).save(any());
    }

    @Test
    @DisplayName("trwająca współpraca blokuje kolejne zaproszenie")
    void shouldRejectInviteWhenCooperationActive() {
        when(userRepository.findByEmail(ATHLETE_EMAIL)).thenReturn(Optional.of(athlete));
        when(cooperationRepository.findByCoachIdAndAthleteIdAndStatusIn(any(), any(), any()))
                .thenReturn(Optional.of(invitation(CooperationStatus.ACTIVE, null)));

        CooperationConflictException ex = assertThrows(CooperationConflictException.class,
                () -> service.invite(COACH_ID, new CooperationInviteRequest(ATHLETE_EMAIL)));

        assertEquals("Prowadzisz już tę osobę", ex.getMessage());
        verify(cooperationRepository, never()).save(any());
    }

    @Test
    @DisplayName("przeterminowane zaproszenie zostaje domknięte i zwalnia parę")
    void shouldExpireStaleInvitationAndProceed() {
        Cooperation stale = invitation(CooperationStatus.PENDING, LocalDateTime.now().minusDays(1));

        when(userRepository.findByEmail(ATHLETE_EMAIL)).thenReturn(Optional.of(athlete));
        when(userRepository.findById(COACH_ID)).thenReturn(Optional.of(coach));
        when(cooperationRepository.findByCoachIdAndAthleteIdAndStatusIn(any(), any(), any()))
                .thenReturn(Optional.of(stale));
        when(cooperationRepository.save(any(Cooperation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.invite(COACH_ID, new CooperationInviteRequest(ATHLETE_EMAIL));

        assertEquals(CooperationStatus.EXPIRED, stale.getStatus());
        verify(cooperationRepository).saveAndFlush(stale);
        verify(cooperationRepository).save(any(Cooperation.class));
    }

    @Test
    @DisplayName("akceptacja przestawia współpracę na aktywną")
    void shouldActivateOnAccept() {
        Cooperation pending = invitation(CooperationStatus.PENDING, LocalDateTime.now().plusDays(7));
        when(cooperationRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(cooperationRepository.save(any(Cooperation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.respond(ATHLETE_ID, 10L, new InvitationDecisionRequest(InvitationDecision.ACCEPTED));

        assertEquals(CooperationStatus.ACTIVE, pending.getStatus());
        assertTrue(pending.getRespondedAt() != null);
    }

    @Test
    @DisplayName("odrzucone zaproszenie nie daje się odrzucić ponownie")
    void shouldRejectSecondDecision() {
        Cooperation resolved = invitation(CooperationStatus.REJECTED, LocalDateTime.now().plusDays(7));
        when(cooperationRepository.findById(10L)).thenReturn(Optional.of(resolved));

        assertThrows(CooperationConflictException.class,
                () -> service.respond(ATHLETE_ID, 10L,
                        new InvitationDecisionRequest(InvitationDecision.ACCEPTED)));

        verify(cooperationRepository, never()).save(any());
    }

    @Test
    @DisplayName("przeterminowanego zaproszenia nie da się zaakceptować")
    void shouldRejectExpiredInvitation() {
        Cooperation stale = invitation(CooperationStatus.PENDING, LocalDateTime.now().minusMinutes(1));
        when(cooperationRepository.findById(10L)).thenReturn(Optional.of(stale));

        assertThrows(CooperationConflictException.class,
                () -> service.respond(ATHLETE_ID, 10L,
                        new InvitationDecisionRequest(InvitationDecision.ACCEPTED)));

        assertEquals(CooperationStatus.PENDING, stale.getStatus());
        verify(cooperationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("odpowiedzieć może wyłącznie adresat")
    void shouldRejectResponseFromSomeoneElse() {
        Cooperation pending = invitation(CooperationStatus.PENDING, LocalDateTime.now().plusDays(7));
        when(cooperationRepository.findById(10L)).thenReturn(Optional.of(pending));

        assertThrows(AccessDeniedException.class,
                () -> service.respond(COACH_ID, 10L,
                        new InvitationDecisionRequest(InvitationDecision.ACCEPTED)));
    }

    @Test
    @DisplayName("nieistniejące zaproszenie daje 404")
    void shouldThrowWhenInvitationMissing() {
        when(cooperationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CooperationNotFoundException.class,
                () -> service.respond(ATHLETE_ID, 99L,
                        new InvitationDecisionRequest(InvitationDecision.REJECTED)));
    }

    @Test
    @DisplayName("lista otrzymanych pomija przeterminowane")
    void shouldHideExpiredFromReceivedList() {
        when(cooperationRepository.findByAthleteIdAndStatus(ATHLETE_ID, CooperationStatus.PENDING))
                .thenReturn(List.of(
                        invitation(CooperationStatus.PENDING, LocalDateTime.now().plusDays(7)),
                        invitation(CooperationStatus.PENDING, LocalDateTime.now().minusDays(1))));

        assertEquals(1, service.receivedInvitations(ATHLETE_ID).size());
    }

    @Test
    @DisplayName("zaproszenie idzie mailem do adresata, z nazwiskiem zapraszającego")
    void shouldSendInvitationEmail() {
        when(userRepository.findByEmail(ATHLETE_EMAIL)).thenReturn(Optional.of(athlete));
        when(userRepository.findById(COACH_ID)).thenReturn(Optional.of(coach));
        givenPairIsFree();
        when(cooperationRepository.save(any(Cooperation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.invite(COACH_ID, new CooperationInviteRequest(ATHLETE_EMAIL));

        verify(emailService).sendCooperationInvitation(
                eq(ATHLETE_EMAIL), eq("Jan Testowy"),
                eq("http://localhost:3000/wspolpraca/zaproszenia"), any());
    }

    @Test
    @DisplayName("niedostępna poczta nie kasuje zaproszenia")
    void shouldKeepInvitationWhenDeliveryFails() {
        when(userRepository.findByEmail(ATHLETE_EMAIL)).thenReturn(Optional.of(athlete));
        when(userRepository.findById(COACH_ID)).thenReturn(Optional.of(coach));
        givenPairIsFree();
        when(cooperationRepository.save(any(Cooperation.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Dostawca niedostępny"))
                .when(emailService).sendCooperationInvitation(any(), any(), any(), any());

        assertEquals(CooperationStatus.PENDING,
                service.invite(COACH_ID, new CooperationInviteRequest(ATHLETE_EMAIL)).status());

        verify(cooperationRepository).save(any(Cooperation.class));
    }


    @Test
    @DisplayName("trener może zakończyć współpracę")
    void shouldAllowCoachToEnd() {
        Cooperation active = invitation(CooperationStatus.ACTIVE, null);
        when(cooperationRepository.findById(10L)).thenReturn(Optional.of(active));

        service.end(COACH_ID, 10L);

        assertEquals(CooperationStatus.ENDED, active.getStatus());
        assertTrue(active.getEndedAt() != null);
        verify(emailService).sendCooperationEnded(eq(ATHLETE_EMAIL), any());
    }

    @Test
    @DisplayName("podopieczny może zakończyć współpracę")
    void shouldAllowAthleteToEnd() {
        Cooperation active = invitation(CooperationStatus.ACTIVE, null);
        when(cooperationRepository.findById(10L)).thenReturn(Optional.of(active));

        service.end(ATHLETE_ID, 10L);

        assertEquals(CooperationStatus.ENDED, active.getStatus());
        // Powiadomienie leci do trenera, czyli do drugiej strony niż przy poprzednim teście.
        verify(emailService).sendCooperationEnded(eq("trener@example.com"), any());
    }

    @Test
    @DisplayName("osoba spoza relacji nie może jej zakończyć")
    void shouldRejectEndByOutsider() {
        Cooperation active = invitation(CooperationStatus.ACTIVE, null);
        when(cooperationRepository.findById(10L)).thenReturn(Optional.of(active));

        assertThrows(AccessDeniedException.class, () -> service.end(99L, 10L));

        assertEquals(CooperationStatus.ACTIVE, active.getStatus());
        verify(cooperationRepository, never()).save(any());
    }

    @Test
    @DisplayName("nieaktywnej współpracy nie da się zakończyć drugi raz")
    void shouldRejectEndWhenNotActive() {
        Cooperation ended = invitation(CooperationStatus.ENDED, null);
        when(cooperationRepository.findById(10L)).thenReturn(Optional.of(ended));

        assertThrows(CooperationConflictException.class, () -> service.end(COACH_ID, 10L));

        verify(cooperationRepository, never()).save(any());
    }

    @Test
    @DisplayName("lista pokazuje, po której stronie relacji stoi pytający")
    void shouldTellWhichSideTheCallerIsOn() {
        when(cooperationRepository.findByParticipantAndStatus(COACH_ID, CooperationStatus.ACTIVE))
                .thenReturn(List.of(invitation(CooperationStatus.ACTIVE, null)));

        CooperationResponse response = service.activeCooperations(COACH_ID).get(0);

        assertEquals(CooperationRole.COACH, response.role());
        assertEquals(ATHLETE_ID, response.partnerId());
    }

    @Test
    @DisplayName("ta sama relacja widziana z drugiej strony ma odwrotną rolę i partnera")
    void shouldFlipRoleForTheOtherSide() {
        when(cooperationRepository.findByParticipantAndStatus(ATHLETE_ID, CooperationStatus.ACTIVE))
                .thenReturn(List.of(invitation(CooperationStatus.ACTIVE, null)));

        CooperationResponse response = service.activeCooperations(ATHLETE_ID).get(0);

        assertEquals(CooperationRole.ATHLETE, response.role());
        assertEquals(COACH_ID, response.partnerId());
    }
}