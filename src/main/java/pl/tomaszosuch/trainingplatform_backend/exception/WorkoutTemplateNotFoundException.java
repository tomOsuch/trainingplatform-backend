package pl.tomaszosuch.trainingplatform_backend.exception;

public class WorkoutTemplateNotFoundException extends RuntimeException {

    public WorkoutTemplateNotFoundException(Long id) {
        super("Nie znaleziono szablonu o identyfikatorze " + id);
    }
}
