package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record CoachModeRequest(
        @NotNull(message = "Wybierz, czy tryb trenera ma być włączony")
        Boolean enabled
) {
}
