package pl.tomaszosuch.trainingplatform_backend.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Nie znaleziono użytkownika o identyfikatorze " + id);
    }

    public UserNotFoundException(String email) {
        super("Nie znaleziono użytkownika o adresie e-mail: " + email);
    }

}