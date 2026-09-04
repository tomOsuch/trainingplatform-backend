package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;

public record GoalStatusUpdateRequest(
        @NotNull(message = "Status jest wymagany")
        GoalStatus status
) {

}
