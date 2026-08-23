package pl.tomaszosuch.trainingplatform_backend.exception;

public class WorkoutCategoryNotFoundException extends RuntimeException {
    public WorkoutCategoryNotFoundException(Long id) {
        super("Nie znaleziono kategorii treningu o identyfikatorze " + id);
    }

    public WorkoutCategoryNotFoundException(String name) {
        super("Nie znaleziono kategorii treningu o nazwie " + name);
    }

}