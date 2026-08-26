package pl.tomaszosuch.trainingplatform_backend.exception;

public class InvitationNotFoundException extends  RuntimeException {

    public InvitationNotFoundException(Long id) {
        super("Nie znaleziono zaproszenia o identyfikatorze " + id);
    }
}
