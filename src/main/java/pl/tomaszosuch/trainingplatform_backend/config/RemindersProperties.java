package pl.tomaszosuch.trainingplatform_backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.reminders")
public class RemindersProperties {

    private boolean enabled;

    @NotBlank
    private String cron;

    @NotNull
    private LocalTime defaultStartTime;
}
