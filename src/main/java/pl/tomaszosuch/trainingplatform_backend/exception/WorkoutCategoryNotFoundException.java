package pl.tomaszosuch.trainingplatform_backend.exception;

public class WorkoutCategoryNotFoundException extends RuntimeException {
    public WorkoutCategoryNotFoundException(Long id) {
        super("Workout category with id " + id + " not found.");
    }
    public WorkoutCategoryNotFoundException(String name) {
        super("Workout category with name " + name + " not found.");
    }

}
