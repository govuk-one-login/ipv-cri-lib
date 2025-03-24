package uk.gov.di.ipv.cri.common.library.exception;

public class RetryException extends RuntimeException {

    public RetryException() {
        super();
    }

    public RetryException(Throwable cause) {
        super(cause);
    }
}
