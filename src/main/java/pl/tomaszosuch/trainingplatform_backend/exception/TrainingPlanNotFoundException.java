package pl.tomaszosuch.trainingplatform_backend.exception;

public class TrainingPlanNotFoundException extends RuntimeException {
    public TrainingPlanNotFoundException(Long id) {
        super("Training plan with id " + id + " not found.");
    }

    public TrainingPlanNotFoundException(String name) {
        super("Training plan with name " + name + " not found.");
    }

}
