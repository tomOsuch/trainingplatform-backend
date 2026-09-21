package pl.tomaszosuch.trainingplatform_backend.exception;

public class PlanAuthorshipException extends RuntimeException {

    public PlanAuthorshipException() {
        super("Możesz edytować tylko plany, które sam ułożyłeś");
    }
}
