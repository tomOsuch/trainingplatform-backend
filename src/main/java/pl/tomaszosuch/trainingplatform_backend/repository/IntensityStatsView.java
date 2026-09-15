package pl.tomaszosuch.trainingplatform_backend.repository;

public interface IntensityStatsView {

    long getTotalCount();

    long getRatedCount();

    long getIntensitySum();
}
