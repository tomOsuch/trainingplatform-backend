package pl.tomaszosuch.trainingplatform_backend.security;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pl.tomaszosuch.trainingplatform_backend.config.RateLimitProperties;
import pl.tomaszosuch.trainingplatform_backend.exception.RateLimitExceededException;

@DisplayName("RateLimiterTest")
public class RateLimiterTest {

    private static final String IP = "192.0.2.10";
    private static final String INNE_IP = "192.0.2.11";
    private static final String TRZECIE_IP = "192.0.2.12";
    private static final String EMAIL = "jan.kowalski@example.com";

    private RateLimitProperties properties;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setLoginPerIp(3);
        properties.setLoginPerAccount(2);
        properties.setLoginWindow(Duration.ofMinutes(15));
        properties.setPasswordResetPerEmail(2);
        properties.setPasswordResetPerIp(5);
        properties.setPasswordResetWindow(Duration.ofMinutes(60));
        properties.setInvitationPerAdmin(2);
        properties.setInvitationWindow(Duration.ofMinutes(60));
        properties.setCooperationInvitationPerUser(3);
        properties.setCooperationInvitationMissesPerUser(2);
        properties.setCooperationInvitationWindow(Duration.ofMinutes(60));

        rateLimiter = new RateLimiter(properties);
    }

    @Test
    @DisplayName("przepuszcza żądania do progu, kolejne odrzuca")
    void shouldBlockAfterThreshold() {
        rateLimiter.checkLoginAttempt(IP, EMAIL);
        rateLimiter.checkLoginAttempt(IP, EMAIL);
        rateLimiter.checkLoginAttempt(IP, EMAIL);

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLoginAttempt(IP, EMAIL));
    }

    @Test
    @DisplayName("wyjątek niesie czas oczekiwania")
    void shouldCarryRetryAfter() {
        rateLimiter.checkLoginAttempt(IP, EMAIL);
        rateLimiter.checkLoginAttempt(IP, EMAIL);
        rateLimiter.checkLoginAttempt(IP, EMAIL);

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLoginAttempt(IP, EMAIL));

        assertTrue(ex.getRetryAfterSeconds() >= 1);
    }

    @Test
    @DisplayName("każdy adres IP ma własny kubełek")
    void shouldKeepSeparateBucketsPerIp() {
        rateLimiter.checkLoginAttempt(IP, EMAIL);
        rateLimiter.checkLoginAttempt(IP, EMAIL);
        rateLimiter.checkLoginAttempt(IP, EMAIL);

        assertDoesNotThrow(() -> rateLimiter.checkLoginAttempt(INNE_IP, EMAIL));
    }

    @Test
    @DisplayName("sama próba logowania NIE zużywa kubełka konta")
    void shouldNotConsumeAccountBucketOnAttempt() {
        rateLimiter.checkLoginAttempt(IP, EMAIL);
        rateLimiter.checkLoginAttempt(IP, EMAIL);
        rateLimiter.checkLoginAttempt(IP, EMAIL);

        assertDoesNotThrow(() -> rateLimiter.checkLoginAttempt(INNE_IP, EMAIL));
    }

    @Test
    @DisplayName("konto blokuje się po nieudanych próbach, także z innych adresów")
    void shouldBlockAccountAcrossIpAddresses() {
        rateLimiter.registerFailedLogin(EMAIL);
        rateLimiter.registerFailedLogin(EMAIL);

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLoginAttempt(INNE_IP, EMAIL));
    }

    @Test
    @DisplayName("wielkość liter i spacje nie tworzą osobnego kubełka konta")
    void shouldNormalizeEmail() {
        rateLimiter.registerFailedLogin("  JAN.KOWALSKI@Example.com  ");
        rateLimiter.registerFailedLogin(EMAIL);

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkLoginAttempt(IP, EMAIL));
    }

    @Test
    @DisplayName("tokeny wracają po upływie okna")
    void shouldRefillAfterWindow() throws InterruptedException {
        properties.setLoginWindow(Duration.ofMillis(500));
        RateLimiter szybki = new RateLimiter(properties);

        szybki.checkLoginAttempt(IP, EMAIL);
        szybki.checkLoginAttempt(IP, EMAIL);
        szybki.checkLoginAttempt(IP, EMAIL);

        assertThrows(RateLimitExceededException.class,
                () -> szybki.checkLoginAttempt(IP, EMAIL));

        Thread.sleep(400);

        assertDoesNotThrow(() -> szybki.checkLoginAttempt(IP, EMAIL));
    }

    @Test
    @DisplayName("wyłączona flaga przepuszcza wszystko")
    void shouldPassEverythingWhenDisabled() {
        properties.setEnabled(false);

        for (int i = 0; i < 50; i++) {
            rateLimiter.checkLoginAttempt(IP, EMAIL);
            rateLimiter.registerFailedLogin(EMAIL);
            rateLimiter.checkPasswordResetRequest(IP, EMAIL);
            rateLimiter.checkInvitationCreation(1L);
        }
    }

    @Test
    @DisplayName("reset hasła jest limitowany po adresie e-mail niezależnie od IP")
    void shouldLimitPasswordResetPerEmail() {
        rateLimiter.checkPasswordResetRequest(IP, EMAIL);
        rateLimiter.checkPasswordResetRequest(INNE_IP, EMAIL);

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkPasswordResetRequest(TRZECIE_IP, EMAIL));
    }

    @Test
    @DisplayName("zaproszenia są limitowane per administrator")
    void shouldLimitInvitationsPerAdmin() {
        rateLimiter.checkInvitationCreation(1L);
        rateLimiter.checkInvitationCreation(1L);

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkInvitationCreation(1L));

        assertDoesNotThrow(() -> rateLimiter.checkInvitationCreation(2L));
    }

    @Test
    @DisplayName("zaproszenia do współpracy są limitowane per użytkownik")
    void shouldLimitCooperationInvitationsPerUser() {
        for (int i = 0; i < 3; i++) {
            rateLimiter.checkCooperationInvitation(1L);
            rateLimiter.refundCooperationInvitationMiss(1L);
        }

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkCooperationInvitation(1L));
        assertDoesNotThrow(() -> rateLimiter.checkCooperationInvitation(2L));
    }

    @Test
    @DisplayName("chybienia blokują wcześniej niż limit ogólny - to jest bariera przeciw enumeracji")
    void shouldBlockAfterMissesEvenWithGeneralRoomLeft() {
        rateLimiter.checkCooperationInvitation(1L);
        rateLimiter.checkCooperationInvitation(1L);

        assertThrows(RateLimitExceededException.class,
                () -> rateLimiter.checkCooperationInvitation(1L));
    }

    @Test
    @DisplayName("trafienie oddaje token chybienia")
    void shouldReturnMissTokenOnHit() {
        rateLimiter.checkCooperationInvitation(1L);
        rateLimiter.refundCooperationInvitationMiss(1L);
        rateLimiter.checkCooperationInvitation(1L);
        rateLimiter.refundCooperationInvitationMiss(1L);

        assertDoesNotThrow(() -> rateLimiter.checkCooperationInvitation(1L));
    }

    @Test
    @DisplayName("równoległe chybienia nie przeskoczą limitu - token jest rezerwowany przed pytaniem o konto")
    void shouldHoldMissLimitUnderConcurrency() throws Exception {
        int watki = 10;
        ExecutorService pula = Executors.newFixedThreadPool(watki);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger przepuszczone = new AtomicInteger();

        List<Future<?>> zadania = new ArrayList<>();
        for (int i = 0; i < watki; i++) {
            zadania.add(pula.submit(() -> {
                start.await();
                try {
                    rateLimiter.checkCooperationInvitation(1L);
                    przepuszczone.incrementAndGet();
                } catch (RateLimitExceededException ignored) {

                }
                return null;
            }));
        }

        start.countDown();
        for (Future<?> zadanie : zadania) {
            zadanie.get(5, TimeUnit.SECONDS);
        }
        pula.shutdown();

        assertEquals(2, przepuszczone.get());
    }
}
