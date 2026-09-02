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

import static org.hibernate.boot.cfgxml.spi.MappingReference.consume;

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

        consume(kluczIp("login", ip), properties.getLoginPerIp(), properties.getLoginWindow());
        assertAvailable(kluczEmail("login", email),
                properties.getLoginPerAccount(), properties.getLoginWindow());
    }

    public void registerFailedLogin(String email) {
        if (!properties.isEnabled()) {
            return;
        }

        penalize(kluczEmail("login", email),
                properties.getLoginPerAccount(), properties.getLoginWindow());
    }

    public void checkPasswordResetRequest(String ip, String email) {
        if (!properties.isEnabled()) {
            return;
        }

        consume(kluczIp("reset", ip),
                properties.getPasswordResetPerIp(), properties.getPasswordResetWindow());
        consume(kluczEmail("reset", email),
                properties.getPasswordResetPerEmail(), properties.getPasswordResetWindow());
    }

    public void checkInvitationCreation(Long adminId) {
        if (!properties.isEnabled()) {
            return;
        }

        consume("zaproszenie:admin:" + adminId,
                properties.getInvitationPerAdmin(), properties.getInvitationWindow());
    }

    private void consume(String klucz, int limit, Duration okno) {
        ConsumptionProbe probe = kubelek(klucz, limit, okno).tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long sekundy = naSekundy(probe.getNanosToWaitForRefill());
            log.warn("Przekroczono limit dla klucza {} — ponowna próba za {} s", klucz, sekundy);
            throw new RateLimitExceededException(sekundy);
        }
    }

    private void assertAvailable(String klucz, int limit, Duration okno) {
        EstimationProbe probe = kubelek(klucz, limit, okno).estimateAbilityToConsume(1);

        if (!probe.canBeConsumed()) {
            long sekundy = naSekundy(probe.getNanosToWaitForRefill());
            log.warn("Klucz {} jest wyczerpany — ponowna próba za {} s", klucz, sekundy);
            throw new RateLimitExceededException(sekundy);
        }
    }

    private void penalize(String klucz, int limit, Duration okno) {
        kubelek(klucz, limit, okno).tryConsume(1);
    }

    private Bucket kubelek(String klucz, int limit, Duration okno) {
        return kubelki.get(klucz, nowy -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, okno)
                        .build())
                .build());
    }

    private String kluczIp(String obszar, String ip) {
        return obszar + ":ip:" + (ip == null || ip.isBlank() ? NIEZNANY : ip);
    }

    private String kluczEmail(String obszar, String email) {
        if (email == null || email.isBlank()) {
            return obszar + ":email:" + NIEZNANY;
        }
        return obszar + ":email:" + email.trim().toLowerCase(Locale.ROOT);
    }

    private long naSekundy(long nanosekundy) {
        return Math.max(1, (nanosekundy + 999_999_999L) / 1_000_000_000L);
    }

}
