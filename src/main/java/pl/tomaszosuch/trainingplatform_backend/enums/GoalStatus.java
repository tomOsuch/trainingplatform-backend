package pl.tomaszosuch.trainingplatform_backend.enums;

public enum GoalStatus {

    ACTIVE,
    ACHIEVED;

    public static GoalStatus fromParam(String value) {
        try {
            return GoalStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Nieznany status celu: " + value + " (dozwolone: active, achieved)");
        }
    }
}
