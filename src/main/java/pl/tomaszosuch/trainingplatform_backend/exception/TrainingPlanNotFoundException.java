package pl.tomaszosuch.trainingplatform_backend.exception;

public class TrainingPlanNotFoundException extends RuntimeException {
    public TrainingPlanNotFoundException(Long id) {
        super("Nie znaleziono planu treningowego o identyfikatorze " + id);
    }

    public TrainingPlanNotFoundException(String name) {
        super("Nie znaleziono planu treningowego o nazwie " + name);
    }

}