package pl.tomaszosuch.trainingplatform_backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.tomaszosuch.trainingplatform_backend.config.RateLimitProperties;
import pl.tomaszosuch.trainingplatform_backend.exception.RateLimitExceededException;

import java.time.Duration;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final Duration BEZCZYNNOSC = Duration.ofHours(2);

    private static final long MAKSIMUM_KUBELKOW = 100_000;
    private static final String NIEZNANY = "nieznany";

    private final RateLimitProperties properties;

    private final Cache<String, Bucket> kubelki = Caffeine.newBuilder()
            .expireAfterAccess(BEZCZYNNOSC)
            .maximumSize(MAKSIMUM_KUBELKOW)
            .build();

    public void checkLoginAttempt(String ip, String email) {
        if (!properties.isEnabled()) {
            return;
        }

        consume(keyIp("login", ip), properties.getLoginPerIp(), properties.getLoginWindow());
        assertAvailable(keyEmail("login", email),
                properties.getLoginPerAccount(), properties.getLoginWindow());
    }

    public void registerFailedLogin(String email) {
        if (!properties.isEnabled()) {
            return;
        }

        penalize(keyEmail("login", email),
                properties.getLoginPerAccount(), properties.getLoginWindow());
    }

    public void checkPasswordResetRequest(String ip, String email) {
        if (!properties.isEnabled()) {
            return;
        }

        consume(keyIp("reset", ip),
                properties.getPasswordResetPerIp(), properties.getPasswordResetWindow());
        consume(keyEmail("reset", email),
                properties.getPasswordResetPerEmail(), properties.getPasswordResetWindow());
    }

    public void checkInvitationCreation(Long adminId) {
        if (!properties.isEnabled()) {
            return;
        }

        consume("zaproszenie:admin:" + adminId,
                properties.getInvitationPerAdmin(), properties.getInvitationWindow());
    }

    public void checkCooperationInvitation(Long userId) {
        if (!properties.isEnabled()) {
            return;
        }

        consume(integrationKey("wszystkie", userId),
                properties.getCooperationInvitationPerUser(),
                properties.getCooperationInvitationWindow());

        consume(integrationKey("chybienia", userId),
                properties.getCooperationInvitationMissesPerUser(),
                properties.getCooperationInvitationWindow());
    }

    public void refundCooperationInvitationMiss(Long userId) {
        if (!properties.isEnabled()) {
            return;
        }

        refund(integrationKey("chybienia", userId),
                properties.getCooperationInvitationMissesPerUser(),
                properties.getCooperationInvitationWindow());
    }

    private void consume(String klucz, int limit, Duration okno) {
        ConsumptionProbe probe = bucket(klucz, limit, okno).tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long sekundy = forSeconds(probe.getNanosToWaitForRefill());
            log.warn("Przekroczono limit dla klucza {} — ponowna próba za {} s", klucz, sekundy);
            throw new RateLimitExceededException(sekundy);
        }
    }

    private void assertAvailable(String klucz, int limit, Duration okno) {
        EstimationProbe probe = bucket(klucz, limit, okno).estimateAbilityToConsume(1);

        if (!probe.canBeConsumed()) {
            long sekundy = forSeconds(probe.getNanosToWaitForRefill());
            log.warn("Klucz {} jest wyczerpany — ponowna próba za {} s", klucz, sekundy);
            throw new RateLimitExceededException(sekundy);
        }
    }

    private String integrationKey(String type, Long userId) {
        return "wspolpraca:" + type + ":uzytkownik:" + userId;
    }

    private void penalize(String klucz, int limit, Duration okno) {
        bucket(klucz, limit, okno).tryConsume(1);
    }

    private void refund(String klucz, int limit, Duration okno) {
        bucket(klucz, limit, okno).addTokens(1);
    }

    private Bucket bucket(String klucz, int limit, Duration okno) {
        return kubelki.get(klucz, nowy -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, okno)
                        .build())
                .build());
    }

    private String keyIp(String obszar, String ip) {
        return obszar + ":ip:" + (ip == null || ip.isBlank() ? NIEZNANY : ip);
    }

    private String keyEmail(String obszar, String email) {
        if (email == null || email.isBlank()) {
            return obszar + ":email:" + NIEZNANY;
        }
        return obszar + ":email:" + email.trim().toLowerCase(Locale.ROOT);
    }

    private long forSeconds(long nanosekundy) {
        return Math.max(1, (nanosekundy + 999_999_999L) / 1_000_000_000L);
    }

}
