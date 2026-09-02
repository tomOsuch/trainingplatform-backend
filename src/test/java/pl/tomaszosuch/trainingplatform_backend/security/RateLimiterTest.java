package pl.tomaszosuch.trainingplatform_backend.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

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

        // Nowa instancja w każdym teście — kubełki żyją w polu, więc wspólny
        // obiekt przenosiłby zużycie między przypadkami i wyniki zależałyby
        // od kolejności wykonania.
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

        // Retry-After: 0 zachęcałby klienta do natychmiastowego ponowienia.
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
        // Limit konta to 2, a mimo trzech prób konto pozostaje dostępne.
        // Gdyby było inaczej, ktoś znający cudzy adres e-mail wykluczyłby
        // właściciela z konta samymi próbami logowania.
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

        // To jest powód, dla którego sam limit po IP nie wystarcza:
        // atak z wielu adresów omijałby go w całości.
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

        // Regeneracja jest płynna: przy 3 tokenach na 500 ms jeden wraca
        // co ~167 ms. Czekamy 400 ms, więc margines jest ponad dwukrotny.
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

        // Limit po IP to 5, więc blokadę wywołuje wyłącznie kubełek adresu e-mail
        // — to on chroni cudzą skrzynkę przed zasypaniem.
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
}