package pl.tomaszosuch.trainingplatform_backend.exception;

public class SelfDeactivationException extends RuntimeException {
    public SelfDeactivationException() {
        super("Nie możesz wyłączyć własnego konta");
    }
}
