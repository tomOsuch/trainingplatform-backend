package pl.tomaszosuch.trainingplatform_backend.exception;

public class InvitationAlreadyResolvedException extends RuntimeException {

    public InvitationAlreadyResolvedException(String message) {
        super(message);
    }
}
