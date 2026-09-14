package pl.tomaszosuch.trainingplatform_backend.repository;

public interface CategoryStatsView {

    Long getCategoryId();

    String getCategoryName();

    String getCategoryColor();

    String getCategoryIconName();

    Long getSessions();

    Long getMinutes();
}
