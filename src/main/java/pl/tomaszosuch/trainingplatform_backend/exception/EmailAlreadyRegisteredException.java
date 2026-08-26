package pl.tomaszosuch.trainingplatform_backend.exception;

public class EmailAlreadyRegisteredException extends  RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Adres " + email + " ma już konto w systemie");
    }
}
