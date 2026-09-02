package pl.tomaszosuch.trainingplatform_backend.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled;

    @Positive
    private int loginPerIp;

    @Positive
    private int loginPerAccount;

    @NotNull
    private Duration loginWindow;

    @Positive
    private int passwordResetPerEmail;

    @Positive
    private int passwordResetPerIp;

    @NotNull
    private Duration passwordResetWindow;

    @Positive
    private int invitationPerAdmin;

    @NotNull
    private Duration invitationWindow;
}
