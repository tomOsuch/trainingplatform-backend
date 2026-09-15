package pl.tomaszosuch.trainingplatform_backend.service.model;

public record PeriodSummary(
        long totalCount,
        long ratedCount,
        long intensitySum,
        long plannedCount
) {

    public IntensitySummary intensity() {
        return new IntensitySummary(totalCount, ratedCount, intensitySum);
    }

    public long adHocCount() {
        return totalCount - plannedCount;
    }
}