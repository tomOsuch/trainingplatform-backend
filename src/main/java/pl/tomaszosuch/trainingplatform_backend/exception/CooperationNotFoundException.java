package pl.tomaszosuch.trainingplatform_backend.exception;

public class CooperationNotFoundException extends RuntimeException {
    public CooperationNotFoundException(Long id) {
        super("Nie znaleziono zaproszenia o identyfikatorze " + id);
    }
}
