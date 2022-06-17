package uk.gov.di.ipv.cri.common.library.exception;

public class AuthorizationCodeExpiredException extends RuntimeException {

    public AuthorizationCodeExpiredException(String message) {
        super(message);
    }

    public AuthorizationCodeExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthorizationCodeExpiredException(Throwable cause) {
        super(cause);
    }
}
