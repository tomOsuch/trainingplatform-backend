package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesRequest(
        @NotNull(message = "Ustawienie przypomnień jest wymagane")
        Boolean remindersEnabled,

        @NotNull(message = "Wyprzedzenie przypomnienia jest wymagane")
        @Min(value = 1, message = "Wyprzedzenie musi wynosić co najmniej 1 godzinę")
        @Max(value = 168, message = "Wyprzedzenie może wynosić maksymalnie 168 godzin")
        Integer reminderHoursBefore
) {
}
