package uk.gov.di.ipv.cri.common.library.config;

import java.util.Optional;

public final class Environment {
    private static final String MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT =
            "Environment variable %s is not set";

    public static String getEnv(String key) {
        return Optional.ofNullable(System.getenv(key))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format(MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT, key)));
    }

    public static String getEnvOrDefault(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key)).orElse(defaultValue);
    }
}
