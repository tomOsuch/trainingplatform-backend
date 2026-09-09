package pl.tomaszosuch.trainingplatform_backend.dto.response;

public record PlanCompletionResponse(
        long completed,
        long skipped,
        long cancelled,
        long unresolved,
        long completionBase,
        Integer completionRate
) {
}
