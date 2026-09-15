package pl.tomaszosuch.trainingplatform_backend.service.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record IntensitySummary(
        long totalCount,
        long ratedCount,
        long intensitySum
) {

    public BigDecimal average() {
        return ratedCount == 0
                ? null
                : BigDecimal.valueOf(intensitySum)
                .divide(BigDecimal.valueOf(ratedCount), 1, RoundingMode.HALF_UP);
    }
}
