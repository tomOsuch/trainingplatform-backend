package pl.tomaszosuch.trainingplatform_backend.exception;

public class WorkoutLogNotFoundException extends RuntimeException {
    public WorkoutLogNotFoundException(Long id) {
        super("Nie znaleziono wpisu w dzienniku o identyfikatorze " + id);
    }
}