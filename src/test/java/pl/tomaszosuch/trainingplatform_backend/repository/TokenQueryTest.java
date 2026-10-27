package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import pl.tomaszosuch.trainingplatform_backend.entity.PasswordResetToken;
import pl.tomaszosuch.trainingplatform_backend.entity.RefreshToken;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.support.PostgresContainerSupport;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("TokenQueryTest")
class TokenQueryTest extends PostgresContainerSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 26, 12, 0);
    private static final LocalDateTime EARLIER = LocalDateTime.of(2026, 10, 1, 8, 30);
    private static final LocalDateTime FUTURE = LocalDateTime.of(2030, 1, 1, 0, 0);
    private static final LocalDateTime PAST = LocalDateTime.of(2020, 1, 1, 0, 0);

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String HASH_C = "c".repeat(64);
    private static final String HASH_D = "d".repeat(64);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = em.persist(user("wlasciciel@example.com"));
        otherUser = em.persist(user("obcy@example.com"));
    }

    private static User user(String email) {
        return User.builder()
                .email(email).password("hash").firstName("Jan").lastName("Testowy")
                .role(Role.USER).isActive(true)
                .build();
    }

    private RefreshToken refreshToken(User user, String hash, LocalDateTime expiresAt, LocalDateTime revokedAt) {
        return em.persist(RefreshToken.builder()
                .user(user).tokenHash(hash).expiresAt(expiresAt).revokedAt(revokedAt)
                .build());
    }

    @Test
    @DisplayName("refresh: token znaleziony po hashu ma załadowanego użytkownika poza kontekstem")
    void shouldFetchUserWithRefreshToken() {
        refreshToken(owner, HASH_A, FUTURE, null);

        em.flush();
        em.clear();

        RefreshToken found = refreshTokenRepository.findByTokenHashWithUser(HASH_A).orElseThrow();

        em.clear();

        assertTrue(Hibernate.isInitialized(found.getUser()));
        assertEquals("wlasciciel@example.com", found.getUser().getEmail());
    }

    @Test
    @DisplayName("kontrola: bez JOIN FETCH użytkownik tokenu NIE jest załadowany")
    void shouldLeaveUserAsProxyWithoutJoinFetch() {
        Long id = refreshToken(owner, HASH_A, FUTURE, null).getId();
        em.flush();
        em.clear();

        RefreshToken found = refreshTokenRepository.findById(id).orElseThrow();
        em.clear();

        assertFalse(Hibernate.isInitialized(found.getUser()));
    }

    @Test
    @DisplayName("refresh: hash wskazuje dokładnie jeden token, właściwego użytkownika")
    void shouldFindRefreshTokenOfRightUser() {
        refreshToken(owner, HASH_A, FUTURE, null);
        refreshToken(otherUser, HASH_B, FUTURE, null);
        em.flush();
        em.clear();

        RefreshToken found = refreshTokenRepository.findByTokenHashWithUser(HASH_B).orElseThrow();

        assertEquals(otherUser.getId(), found.getUser().getId());
    }

    @Test
    @DisplayName("refresh: nieznany hash daje pusty wynik, nie wyjątek")
    void shouldReturnEmptyForUnknownRefreshHash() {
        refreshToken(owner, HASH_A, FUTURE, null);
        em.flush();
        em.clear();

        assertTrue(refreshTokenRepository.findByTokenHashWithUser(HASH_D).isEmpty());
    }

    @Test
    @DisplayName("refresh: zwraca także token unieważniony — serwis rozpoznaje po nim ponowne użycie")
    void shouldReturnRevokedRefreshToken() {
        refreshToken(owner, HASH_A, FUTURE, EARLIER);
        em.flush();
        em.clear();

        RefreshToken found = refreshTokenRepository.findByTokenHashWithUser(HASH_A).orElseThrow();

        assertEquals(EARLIER, found.getRevokedAt());
    }

    @Test
    @DisplayName("revokeAll: unieważnia aktywne tokeny właściciela i tylko je")
    void shouldRevokeOnlyActiveTokensOfUser() {
        Long active = refreshToken(owner, HASH_A, FUTURE, null).getId();
        Long expired = refreshToken(owner, HASH_B, PAST, null).getId();
        Long alreadyRevoked = refreshToken(owner, HASH_C, FUTURE, EARLIER).getId();
        Long foreign = refreshToken(otherUser, HASH_D, FUTURE, null).getId();
        em.flush();

        int revoked = refreshTokenRepository.revokeAllActiveByUserId(owner.getId(), NOW);

        assertEquals(2, revoked);
        assertEquals(NOW, em.find(RefreshToken.class, active).getRevokedAt());
        assertEquals(NOW, em.find(RefreshToken.class, expired).getRevokedAt());
        assertEquals(EARLIER, em.find(RefreshToken.class, alreadyRevoked).getRevokedAt());
        assertNull(em.find(RefreshToken.class, foreign).getRevokedAt());
    }

    @Test
    @DisplayName("revokeAll: użytkownik bez aktywnych tokenów — zero zmian")
    void shouldRevokeNothingWhenNoActiveTokens() {
        refreshToken(owner, HASH_A, FUTURE, EARLIER);
        em.flush();

        assertEquals(0, refreshTokenRepository.revokeAllActiveByUserId(owner.getId(), NOW));
    }

    @Test
    @DisplayName("reset hasła: token po hashu ma załadowanego użytkownika poza kontekstem")
    void shouldFetchUserWithPasswordResetToken() {
        em.persist(PasswordResetToken.builder()
                .user(owner).tokenHash(HASH_A).expiresAt(FUTURE)
                .build());

        em.flush();
        em.clear();

        PasswordResetToken found = passwordResetTokenRepository.findByTokenHashWithUser(HASH_A).orElseThrow();
        em.clear();

        assertTrue(Hibernate.isInitialized(found.getUser()));
        assertEquals("wlasciciel@example.com", found.getUser().getEmail());
    }

    @Test
    @DisplayName("reset hasła: hash wskazuje token właściwego użytkownika, nieznany daje pusty wynik")
    void shouldFindPasswordResetTokenOfRightUser() {
        em.persist(PasswordResetToken.builder()
                .user(owner).tokenHash(HASH_A).expiresAt(FUTURE).build());
        em.persist(PasswordResetToken.builder()
                .user(otherUser).tokenHash(HASH_B).expiresAt(FUTURE).build());
        em.flush();
        em.clear();

        assertEquals(otherUser.getId(),
                passwordResetTokenRepository.findByTokenHashWithUser(HASH_B).orElseThrow().getUser().getId());
        assertTrue(passwordResetTokenRepository.findByTokenHashWithUser(HASH_D).isEmpty());
    }

    @Test
    @DisplayName("reset hasła: zwraca także token już wykorzystany — o ważności decyduje serwis")
    void shouldReturnUsedPasswordResetToken() {
        em.persist(PasswordResetToken.builder()
                .user(owner).tokenHash(HASH_A).expiresAt(FUTURE).usedAt(EARLIER)
                .build());
        em.flush();
        em.clear();

        assertEquals(EARLIER,
                passwordResetTokenRepository.findByTokenHashWithUser(HASH_A).orElseThrow().getUsedAt());
    }

}