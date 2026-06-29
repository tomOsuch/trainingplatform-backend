package pl.tomaszosuch.trainingplatform_backend.exception;

public class WorkoutLogNotFoundException extends RuntimeException {
    public WorkoutLogNotFoundException(Long id) {
        super("Workout log with id " + id + " not found.");
    }   

    public WorkoutLogNotFoundException(String name) {
        super("Workout log with name " + name + " not found.");
    }

}
