package pl.tomaszosuch.trainingplatform_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.admin")
public class AdminBootstrapProperties {

    private String email;
    private String password;
    private String firstName = "Administrator";
    private String lastName = "Systemu";
}
