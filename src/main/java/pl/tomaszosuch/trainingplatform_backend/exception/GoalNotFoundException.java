package pl.tomaszosuch.trainingplatform_backend.exception;

public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException(Long id) {
        super("Nie znaleziono celu o identyfikatorze " + id);
    }
}
