package pl.tomaszosuch.trainingplatform_backend.exception;

public class LastAdminException extends RuntimeException {

    public LastAdminException() {
        super("Nie można usunąć konta ostatniego administratora");
    }
}
