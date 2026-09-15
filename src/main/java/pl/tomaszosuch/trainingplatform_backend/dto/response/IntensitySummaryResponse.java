package pl.tomaszosuch.trainingplatform_backend.dto.response;

import java.math.BigDecimal;

public record IntensitySummaryResponse(
        BigDecimal average,
        long ratedCount,
        long totalCount
) {
}
