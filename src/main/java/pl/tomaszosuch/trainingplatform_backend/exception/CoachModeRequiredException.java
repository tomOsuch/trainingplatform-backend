package pl.tomaszosuch.trainingplatform_backend.exception;

public class CoachModeRequiredException extends RuntimeException {

    public CoachModeRequiredException() {
        super("Zapraszać podopiecznych może konto z włączonym trybem trenera — włączysz go w profilu");
    }
}
