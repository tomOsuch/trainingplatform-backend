package pl.tomaszosuch.trainingplatform_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    @Positive
    private int expirationMinutes;

    @NotBlank
    private String resetBaseUrl;
}
