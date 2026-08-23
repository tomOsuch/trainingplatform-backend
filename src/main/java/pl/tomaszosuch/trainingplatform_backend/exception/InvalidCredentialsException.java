package pl.tomaszosuch.trainingplatform_backend.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Nieprawidłowy e-mail lub hasło");
    }
}
