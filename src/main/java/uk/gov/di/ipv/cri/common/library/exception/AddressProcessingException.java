package uk.gov.di.ipv.cri.common.library.exception;

public class AddressProcessingException extends Exception {

    public AddressProcessingException(String message) {
        super(message);
    }

    public AddressProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public AddressProcessingException(Throwable cause) {
        super(cause);
    }
}
