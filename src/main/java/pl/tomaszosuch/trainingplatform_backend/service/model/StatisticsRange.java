package pl.tomaszosuch.trainingplatform_backend.service.model;

import java.time.LocalDate;
import java.time.YearMonth;

public record StatisticsRange(LocalDate from, LocalDate to) {

    private static final String PARTIAL_RANGE_MESSAGE =
            "Podaj obie granice zakresu albo żadnej — wtedy statystyki obejmą bieżący miesiąc";

    public static StatisticsRange of(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            YearMonth currentMonth = YearMonth.now();
            return new StatisticsRange(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException(PARTIAL_RANGE_MESSAGE);
        }
        return new StatisticsRange(from, to);
    }
}