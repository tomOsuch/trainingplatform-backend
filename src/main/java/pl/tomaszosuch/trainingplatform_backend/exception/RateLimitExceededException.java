package pl.tomaszosuch.trainingplatform_backend.exception;

public class RateLimitExceededException extends  RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Zbyt wiele prób. Spróbuj ponownie za chwilę.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
