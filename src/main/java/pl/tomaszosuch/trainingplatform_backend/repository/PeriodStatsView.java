package pl.tomaszosuch.trainingplatform_backend.repository;

public interface PeriodStatsView {

    long getTotalCount();

    long getRatedCount();

    long getIntensitySum();

    long getPlannedCount();
}
