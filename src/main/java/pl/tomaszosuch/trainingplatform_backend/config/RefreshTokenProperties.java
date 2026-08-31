package pl.tomaszosuch.trainingplatform_backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public class RefreshTokenProperties {

    @Positive
    private int expirationDays;

    @NotBlank
    private String cookieName;

    @NotBlank
    private String cookiePath;

    private boolean cookieSecure;

    @NotBlank
    private String cookieSameSite;
}
