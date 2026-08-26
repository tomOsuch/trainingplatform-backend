package pl.tomaszosuch.trainingplatform_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.invitation")
public class InvitationProperties {

    private int expirationDays;
    private String acceptBaseUrl = "http://localhost:5173/register";
}
