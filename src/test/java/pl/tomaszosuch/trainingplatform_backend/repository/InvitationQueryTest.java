package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import pl.tomaszosuch.trainingplatform_backend.entity.Invitation;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.support.PostgresContainerSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("InvitationQueryTest")
class InvitationQueryTest extends PostgresContainerSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
    private static final LocalDateTime EARLIER = LocalDateTime.of(2026, 10, 1, 8, 30);
    private static final LocalDateTime FUTURE = LocalDateTime.of(2030, 1, 1, 0, 0);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private InvitationRepository invitationRepository;

    private User admin;
    private User otherAdmin;

    private int hashSeq;

    @BeforeEach
    void setUp() {
        admin = em.persist(user("admin@example.com"));
        otherAdmin = em.persist(user("drugi-admin@example.com"));
        hashSeq = 0;
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.ADMIN).isActive(true)
                .build();
    }

    private Invitation invitation(User invitedBy, String email, LocalDateTime usedAt, LocalDateTime revokedAt) {
        return em.persist(Invitation.builder()
                .email(email)
                .tokenHash(String.format("%064d", ++hashSeq))
                .role(Role.USER)
                .invitedBy(invitedBy)
                .expiresAt(FUTURE)
                .usedAt(usedAt)
                .revokedAt(revokedAt)
                .build());
    }

    private void setCreatedAt(Invitation invitation, LocalDateTime createdAt) {
        em.getEntityManager()
                .createNativeQuery("UPDATE invitation SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, invitation.getId())
                .executeUpdate();
    }

    private void deleteUserInDatabase(User user) {
        em.getEntityManager()
                .createNativeQuery("DELETE FROM users WHERE id = ?1")
                .setParameter(1, user.getId())
                .executeUpdate();
    }

    @Test
    @DisplayName("lista zachowuje zaproszenia usuniętego nadawcy — LEFT JOIN, nie JOIN")
    void shouldKeepInvitationsOfDeletedInviter() {
        Invitation fromAdmin = invitation(admin, "anna@example.com", null, null);
        Invitation orphan = invitation(otherAdmin, "piotr@example.com", null, null);
        em.flush();

        deleteUserInDatabase(otherAdmin);

        em.clear();

        List<Invitation> result = invitationRepository.findAllWithInviter();
        em.clear();

        assertEquals(2, result.size());

        Invitation orphanFound = result.stream()
                .filter(i -> i.getId().equals(orphan.getId())).findFirst().orElseThrow();
        assertNull(orphanFound.getInvitedBy());

        Invitation adminFound = result.stream()
                .filter(i -> i.getId().equals(fromAdmin.getId())).findFirst().orElseThrow();
        assertEquals(admin.getId(), adminFound.getInvitedBy().getId());
    }

    @Test
    @DisplayName("nadawca zaproszenia jest załadowany poza kontekstem")
    void shouldFetchInviter() {
        invitation(admin, "anna@example.com", null, null);

        em.flush();
        em.clear();

        Invitation found = invitationRepository.findAllWithInviter().getFirst();
        em.clear();

        assertTrue(Hibernate.isInitialized(found.getInvitedBy()));
        assertEquals("admin@example.com", found.getInvitedBy().getEmail());
    }

    @Test
    @DisplayName("lista jest posortowana od najnowszego zaproszenia")
    void shouldSortNewestFirst() {
        Invitation older = invitation(admin, "anna@example.com", null, null);
        Invitation newer = invitation(admin, "piotr@example.com", null, null);
        Invitation middle = invitation(otherAdmin, "ewa@example.com", null, null);
        em.flush();
        setCreatedAt(older, LocalDateTime.of(2026, 9, 1, 10, 0));
        setCreatedAt(middle, LocalDateTime.of(2026, 9, 15, 10, 0));
        setCreatedAt(newer, LocalDateTime.of(2026, 10, 1, 10, 0));
        em.clear();

        List<Long> ids = invitationRepository.findAllWithInviter().stream()
                .map(Invitation::getId).toList();

        assertEquals(List.of(newer.getId(), middle.getId(), older.getId()), ids);
    }

    @Test
    @DisplayName("revokePending: unieważnia oczekujące zaproszenia nadawcy i tylko je")
    void shouldRevokeOnlyPendingInvitationsOfInviter() {
        Long pending = invitation(admin, "anna@example.com", null, null).getId();
        Long used = invitation(admin, "piotr@example.com", EARLIER, null).getId();
        Long alreadyRevoked = invitation(admin, "ewa@example.com", null, EARLIER).getId();
        Long foreign = invitation(otherAdmin, "marek@example.com", null, null).getId();
        em.flush();

        int revoked = invitationRepository.revokePendingByInviter(admin.getId(), NOW);

        assertEquals(1, revoked);
        assertEquals(NOW, em.find(Invitation.class, pending).getRevokedAt());
        assertNull(em.find(Invitation.class, used).getRevokedAt());
        assertEquals(EARLIER, em.find(Invitation.class, alreadyRevoked).getRevokedAt());
        assertNull(em.find(Invitation.class, foreign).getRevokedAt());
    }

    @Test
    @DisplayName("revokePending: unieważnione zaproszenie zwalnia adres dla nowego zaproszenia")
    void shouldFreeEmailForNewInvitationAfterRevoke() {
        invitation(admin, "anna@example.com", null, null);
        em.flush();

        invitationRepository.revokePendingByInviter(admin.getId(), NOW);

        invitation(otherAdmin, "anna@example.com", null, null);
        em.flush();
    }

}