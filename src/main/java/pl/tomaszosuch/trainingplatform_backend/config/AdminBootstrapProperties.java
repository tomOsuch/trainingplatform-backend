package pl.tomaszosuch.trainingplatform_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.admin")
public class AdminBootstrapProperties {

    private String email;
    private String password;
    private String firstName;
    private String lastName;
}
