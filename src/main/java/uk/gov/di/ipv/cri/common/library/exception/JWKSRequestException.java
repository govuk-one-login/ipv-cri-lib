package uk.gov.di.ipv.cri.common.library.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JWKSRequestException extends Exception {
    private static final Logger LOGGER = LoggerFactory.getLogger(JWKSRequestException.class);

    public JWKSRequestException(String message) {
        super(message);
    }

    public JWKSRequestException(String message, Throwable cause) {
        super(message, cause);
        LOGGER.error(
                "{} called with cause {}: {}",
                JWKSRequestException.class.getSimpleName(),
                cause.getClass().getName(),
                cause.getMessage());
    }
}
