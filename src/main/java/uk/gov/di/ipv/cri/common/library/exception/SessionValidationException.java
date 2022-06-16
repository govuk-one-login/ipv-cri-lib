package uk.gov.di.ipv.cri.common.library.exception;

public class SessionValidationException extends RuntimeException {
    public SessionValidationException(String message, Exception e) {
        super(message, e);
    }

    public SessionValidationException(String message) {
        super(message);
    }
}
