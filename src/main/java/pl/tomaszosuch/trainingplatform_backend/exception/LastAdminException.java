package pl.tomaszosuch.trainingplatform_backend.exception;

public class LastAdminException extends RuntimeException {

    public LastAdminException() {
        super("To ostatnie aktywne konto administratora — bez niego nikt nie odzyska dostępu do panelu");
    }
}