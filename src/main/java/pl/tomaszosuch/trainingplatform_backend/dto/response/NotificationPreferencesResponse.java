package pl.tomaszosuch.trainingplatform_backend.dto.response;

public record NotificationPreferencesResponse(
        boolean remindersEnabled,
        int reminderHoursBefore
) {
}
