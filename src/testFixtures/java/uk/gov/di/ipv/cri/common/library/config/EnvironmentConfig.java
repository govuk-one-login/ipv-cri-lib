package uk.gov.di.ipv.cri.common.library.config;

import java.util.Optional;

public final class EnvironmentConfig {
    public static String getEnvironment(String key) {
        return Optional.ofNullable(System.getenv(key))
                .orElseThrow(() -> new IllegalArgumentException("Missing env variable: " + key));
    }
}
