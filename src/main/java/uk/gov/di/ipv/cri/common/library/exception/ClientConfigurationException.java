package uk.gov.di.ipv.cri.common.library.exception;

public class ClientConfigurationException extends Exception {
    public ClientConfigurationException(Exception e) {
        super(e);
    }

    public ClientConfigurationException(String message, Exception e) {
        super(message, e);
    }
}
