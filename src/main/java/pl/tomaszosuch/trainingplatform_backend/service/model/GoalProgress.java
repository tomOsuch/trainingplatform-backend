package pl.tomaszosuch.trainingplatform_backend.service.model;

public record GoalProgress(long currentValue, long targetValue) {

    public int percent() {
        return (int) Math.min(100L, currentValue * 100L / targetValue);
    }

    public boolean targetReached() {
        return currentValue >= targetValue;
    }
}
