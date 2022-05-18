package uk.gov.di.ipv.cri.common.library.exception;

public class AccessTokenValidationException extends Exception {
    public AccessTokenValidationException(Throwable cause) {
        super(cause);
    }

    public AccessTokenValidationException(String message) {
        super(message);
    }
}
