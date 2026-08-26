package pl.tomaszosuch.trainingplatform_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String provider = "log";
    private String from = "zaproszenia@trainingplatform.local";
}
