package pl.tomaszosuch.trainingplatform_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.tomaszosuch.trainingplatform_backend.config.InvitationProperties;
import pl.tomaszosuch.trainingplatform_backend.dto.request.InvitationRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.InvitationResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Invitation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.InvitationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailAlreadyRegisteredException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvitationAlreadyResolvedException;
import pl.tomaszosuch.trainingplatform_backend.exception.InvitationNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.InvitationMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.InvitationRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.security.SecureTokenGenerator;
import pl.tomaszosuch.trainingplatform_backend.service.impl.InvitationServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationServiceImplTest")
public class InvitationServiceImplTest {

    private static final String EMAIL = "nowy@example.com";
    private static final String PLAIN_TOKEN = "abc123-XYZ_jawnyToken";
    private static final String TOKEN_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String ACCEPT_BASE_URL = "http://localhost:5173/register";

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecureTokenGenerator tokenGenerator;

    @Mock
    private InvitationMapper invitationMapper;

    @Mock
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<Invitation> invitationCaptor;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    private InvitationProperties properties;
    private InvitationServiceImpl invitationService;

    private User admin;
    private InvitationResponse response;

    @BeforeEach
    void setUp() {
        properties = new InvitationProperties();
        properties.setExpirationDays(7);
        properties.setAcceptBaseUrl(ACCEPT_BASE_URL);

        invitationService = new InvitationServiceImpl(
                invitationRepository,
                userRepository,
                tokenGenerator,
                invitationMapper,
                emailService,
                properties);

        admin = User.builder()
                .id(1L)
                .email("admin@example.com")
                .firstName("Administrator")
                .lastName("Systemu")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        response = new InvitationResponse(
                10L, EMAIL, Role.USER, InvitationStatus.PENDING,
                "admin@example.com",
                LocalDateTime.now().plusDays(7), LocalDateTime.now(), LocalDateTime.now());
    }

    private void stubHappyPath() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(tokenGenerator.generateToken()).thenReturn(PLAIN_TOKEN);
        when(tokenGenerator.hash(PLAIN_TOKEN)).thenReturn(TOKEN_HASH);
        when(invitationRepository.save(any(Invitation.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    @Nested
    @DisplayName("Wystawianie zaproszenia")
    class Creating {

        @Test
        @DisplayName("zapisuje skrót tokenu, nigdy tokenu jawnego")
        void shouldStoreOnlyTokenHash() {
            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.empty());
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);

            invitationService.createInvitation(new InvitationRequest(EMAIL, null), 1L);

            verify(invitationRepository).save(invitationCaptor.capture());
            Invitation saved = invitationCaptor.getValue();

            assertEquals(TOKEN_HASH, saved.getTokenHash());
            assertNotEquals(PLAIN_TOKEN, saved.getTokenHash());
        }

        @Test
        @DisplayName("ustawia termin ważności zgodnie z konfiguracją")
        void shouldSetExpirationFromConfiguration() {
            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.empty());
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);

            invitationService.createInvitation(new InvitationRequest(EMAIL, null), 1L);

            verify(invitationRepository).save(invitationCaptor.capture());
            LocalDateTime expiresAt = invitationCaptor.getValue().getExpiresAt();

            assertTrue(expiresAt.isAfter(LocalDateTime.now().plusDays(6)));
            assertTrue(expiresAt.isBefore(LocalDateTime.now().plusDays(8)));
        }

        @Test
        @DisplayName("nadaje rolę USER, gdy żądanie jej nie zawiera")
        void shouldDefaultToUserRole() {
            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.empty());
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);

            invitationService.createInvitation(new InvitationRequest(EMAIL, null), 1L);

            verify(invitationRepository).save(invitationCaptor.capture());
            assertEquals(Role.USER, invitationCaptor.getValue().getRole());
        }

        @Test
        @DisplayName("zachowuje rolę ADMIN przekazaną w żądaniu")
        void shouldKeepRequestedAdminRole() {
            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.empty());
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);

            invitationService.createInvitation(new InvitationRequest(EMAIL, Role.ADMIN), 1L);

            verify(invitationRepository).save(invitationCaptor.capture());
            assertEquals(Role.ADMIN, invitationCaptor.getValue().getRole());
        }

        @Test
        @DisplayName("unieważnia poprzednie oczekujące zaproszenie PRZED zapisem nowego")
        void shouldRevokePreviousPendingInvitation() {
            Invitation previous = Invitation.builder()
                    .id(5L)
                    .email(EMAIL)
                    .tokenHash("stary-hash")
                    .role(Role.USER)
                    .invitedBy(admin)
                    .expiresAt(LocalDateTime.now().plusDays(3))
                    .build();

            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.of(previous));
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);

            invitationService.createInvitation(new InvitationRequest(EMAIL, null), 1L);

            assertNotNull(previous.getRevokedAt());
            verify(invitationRepository).saveAndFlush(previous);
        }

        @Test
        @DisplayName("odrzuca adres, który ma już konto")
        void shouldRejectEmailWithExistingAccount() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThrows(EmailAlreadyRegisteredException.class,
                    () -> invitationService.createInvitation(new InvitationRequest(EMAIL, null), 1L));

            verify(invitationRepository, never()).save(any(Invitation.class));
            verify(emailService, never()).sendInvitation(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("obcina białe znaki wokół adresu")
        void shouldTrimEmail() {
            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.empty());
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);

            invitationService.createInvitation(new InvitationRequest("  " + EMAIL + "  ", null), 1L);

            verify(invitationRepository).save(invitationCaptor.capture());
            assertEquals(EMAIL, invitationCaptor.getValue().getEmail());
        }

    }

    @Nested
    @DisplayName("Wysyłka zaproszenia")
    class Delivery {

        @Test
        @DisplayName("buduje link z adresu bazowego z konfiguracji i tokenu jawnego")
        void shouldBuildLinkFromConfiguredBaseUrl() {
            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.empty());
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);

            invitationService.createInvitation(new InvitationRequest(EMAIL, null), 1L);

            verify(emailService).sendInvitation(eq(EMAIL), urlCaptor.capture(), any(LocalDateTime.class));
            String url = urlCaptor.getValue();

            assertTrue(url.startsWith(ACCEPT_BASE_URL));
            assertTrue(url.contains(PLAIN_TOKEN));
        }

        @Test
        @DisplayName("ustawia sentAt po udanej wysyłce")
        void shouldMarkAsSentOnSuccess() {
            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.empty());
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);

            invitationService.createInvitation(new InvitationRequest(EMAIL, null), 1L);

            verify(invitationRepository).save(invitationCaptor.capture());
            assertNotNull(invitationCaptor.getValue().getSentAt());
        }

        @Test
        @DisplayName("zapisuje zaproszenie z pustym sentAt, gdy wysyłka zawiedzie")
        void shouldKeepInvitationWhenDeliveryFails() {
            stubHappyPath();
            when(invitationRepository.findByEmailAndUsedAtIsNullAndRevokedAtIsNull(EMAIL))
                    .thenReturn(Optional.empty());
            when(invitationMapper.toResponse(any(Invitation.class))).thenReturn(response);
            doThrow(new RuntimeException("Dostawca niedostępny"))
                    .when(emailService).sendInvitation(anyString(), anyString(), any());

            invitationService.createInvitation(new InvitationRequest(EMAIL, null), 1L);

            verify(invitationRepository).save(invitationCaptor.capture());
            assertNull(invitationCaptor.getValue().getSentAt());
        }
    }

    @Nested
    @DisplayName("Unieważnianie zaproszenia")
    class Revoking {

        @Test
        @DisplayName("ustawia revokedAt")
        void shouldSetRevokedAt() {
            Invitation invitation = Invitation.builder()
                    .id(10L)
                    .email(EMAIL)
                    .tokenHash(TOKEN_HASH)
                    .role(Role.USER)
                    .invitedBy(admin)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .build();

            when(invitationRepository.findById(10L)).thenReturn(Optional.of(invitation));

            invitationService.revokeInvitation(10L);

            assertNotNull(invitation.getRevokedAt());
            verify(invitationRepository).save(invitation);
        }

        @Test
        @DisplayName("rzuca wyjątek dla nieistniejącego zaproszenia")
        void shouldThrowWhenNotFound() {
            when(invitationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(InvitationNotFoundException.class,
                    () -> invitationService.revokeInvitation(99L));
        }

        @Test
        @DisplayName("nie pozwala unieważnić wykorzystanego zaproszenia")
        void shouldRejectUsedInvitation() {
            Invitation invitation = Invitation.builder()
                    .id(10L)
                    .email(EMAIL)
                    .tokenHash(TOKEN_HASH)
                    .role(Role.USER)
                    .invitedBy(admin)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .usedAt(LocalDateTime.now().minusHours(2))
                    .build();

            when(invitationRepository.findById(10L)).thenReturn(Optional.of(invitation));

            assertThrows(InvitationAlreadyResolvedException.class,
                    () -> invitationService.revokeInvitation(10L));

            verify(invitationRepository, never()).save(any(Invitation.class));
        }

        @Test
        @DisplayName("nie pozwala unieważnić zaproszenia już unieważnionego")
        void shouldRejectAlreadyRevokedInvitation() {
            Invitation invitation = Invitation.builder()
                    .id(10L)
                    .email(EMAIL)
                    .tokenHash(TOKEN_HASH)
                    .role(Role.USER)
                    .invitedBy(admin)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .revokedAt(LocalDateTime.now().minusMinutes(30))
                    .build();

            when(invitationRepository.findById(10L)).thenReturn(Optional.of(invitation));

            assertThrows(InvitationAlreadyResolvedException.class,
                    () -> invitationService.revokeInvitation(10L));

            verify(invitationRepository, never()).save(any(Invitation.class));
        }
    }

    @Nested
    @DisplayName("Lista zaproszeń")
    class Listing {

        @Test
        @DisplayName("pobiera zaproszenia razem z inicjatorem")
        void shouldFetchInvitationsWithInviter() {
            Invitation invitation = Invitation.builder()
                    .id(10L)
                    .email(EMAIL)
                    .tokenHash(TOKEN_HASH)
                    .role(Role.USER)
                    .invitedBy(admin)
                    .expiresAt(LocalDateTime.now().plusDays(5))
                    .build();

            when(invitationRepository.findAllWithInviter()).thenReturn(List.of(invitation));
            when(invitationMapper.toResponse(invitation)).thenReturn(response);

            List<InvitationResponse> result = invitationService.findAllInvitations();

            assertEquals(1, result.size());
            verify(invitationRepository).findAllWithInviter();
        }
    }
}
