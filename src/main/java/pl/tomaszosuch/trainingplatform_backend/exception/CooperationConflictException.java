package pl.tomaszosuch.trainingplatform_backend.exception;

public class CooperationConflictException extends RuntimeException {
    public CooperationConflictException(String message) {
        super(message);
    }
}
