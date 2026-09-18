package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import pl.tomaszosuch.trainingplatform_backend.enums.InvitationDecision;

public record InvitationDecisionRequest(

        @NotNull(message = "Decyzja jest wymagana")
        InvitationDecision decision
) {
}
