package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;

public record StatusUpdateRequest(
        @NotNull(message = "Status jest wymagany")
        PlanStatus status
) {

}