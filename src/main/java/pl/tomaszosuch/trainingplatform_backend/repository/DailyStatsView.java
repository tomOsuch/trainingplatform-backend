package pl.tomaszosuch.trainingplatform_backend.repository;

import java.time.LocalDate;

public interface DailyStatsView {

    LocalDate getDay();

    Long getSessions();

    Long getMinutes();
}
