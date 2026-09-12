package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import pl.tomaszosuch.trainingplatform_backend.enums.AccountStatus;

public record AccountStatusUpdateRequest(
        @NotNull(message = "Status jest wymagany")
        AccountStatus status
) {
}
