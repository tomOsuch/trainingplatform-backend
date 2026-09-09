package pl.tomaszosuch.trainingplatform_backend.service.model;

public record CategoryStatistics(
        Long categoryId,
        String categoryName,
        String categoryColor,
        Long sessions,
        Long minutes
) {

}
