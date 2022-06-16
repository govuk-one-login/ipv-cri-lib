package uk.gov.di.ipv.cri.common.library.exception;

public class AccessTokenExpiredException extends Exception {

    public AccessTokenExpiredException(String message) {
        super(message);
    }

    public AccessTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccessTokenExpiredException(Throwable cause) {
        super(cause);
    }
}
