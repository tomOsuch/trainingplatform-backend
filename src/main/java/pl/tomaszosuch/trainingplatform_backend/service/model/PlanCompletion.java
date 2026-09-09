package pl.tomaszosuch.trainingplatform_backend.service.model;

public record PlanCompletion(
        long completed,
        long skipped,
        long cancelled,
        long unresolved
) {

    public long completionBase() {
        return completed + skipped + unresolved;
    }

    public Integer completionRate() {
        long base = completionBase();
        return base == 0 ? null : (int) (completed * 100 / base);
    }
}
