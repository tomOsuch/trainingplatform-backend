package pl.tomaszosuch.trainingplatform_backend.config;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cooperation")
public class CooperationProperties {

    @Positive
    private int invitationExpirationDays;
}
