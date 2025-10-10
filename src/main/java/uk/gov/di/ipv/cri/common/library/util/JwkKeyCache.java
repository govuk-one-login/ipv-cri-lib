package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.domain.jwks.JWKS;
import uk.gov.di.ipv.cri.common.library.domain.jwks.Key;
import uk.gov.di.ipv.cri.common.library.exception.JWKSRequestException;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class JwkKeyCache {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(JwkKeyCache.class);

    private final boolean usePublicJwk;
    private final JwkRequest jwkRequest;

    private final Map<String, JWKS> cachedJwks = new HashMap<>();

    public JwkKeyCache() {
        this(new JwkRequest());
    }

    public JwkKeyCache(JwkRequest jwkRequest) {
        this.jwkRequest = jwkRequest;
        usePublicJwk =
                Boolean.parseBoolean(
                        Optional.ofNullable(System.getenv("ENV_VAR_FEATURE_CONSUME_PUBLIC_JWK"))
                                .orElse("false"));
    }

    public Optional<String> getBase64JwkForKid(String publicJwkEndpoint, String kid) {
        if (!usePublicJwk) {
            LOGGER.info("Using public JWKs endpoint is disabled");
            return Optional.empty();
        }
        if (publicJwkEndpoint == null) {
            LOGGER.error("No JWKS endpoint configured for the client");
            return Optional.empty();
        }

        LOGGER.info("Using JWKs endpoint: {}", publicJwkEndpoint);
        JWKS cachedJwksEndpoint = cachedJwks.get(publicJwkEndpoint);

        if (cachedJwksEndpoint == null
                || System.currentTimeMillis()
                        > cachedJwksEndpoint.getLastUpdated()
                                + cachedJwksEndpoint.getCacheControl()) {
            JWKS newJwks;
            try {
                newJwks = jwkRequest.callJWKSEndpoint(publicJwkEndpoint);
            } catch (JWKSRequestException e) {
                LOGGER.error("Failed to call JWK endpoint ({})", publicJwkEndpoint, e);
                return Optional.empty();
            }
            newJwks.setLastUpdated(System.currentTimeMillis());
            newJwks.setCacheControl(
                    TimeUnit.SECONDS.toMillis(newJwks.getMaxAgeFromCacheControlHeader()));
            LOGGER.info(
                    "JWKs cache has been updated to '{}' seconds",
                    newJwks.getMaxAgeFromCacheControlHeader());
            cachedJwks.put(publicJwkEndpoint, newJwks);
        } else {
            LOGGER.info("Using locally cached JWKs from {}", publicJwkEndpoint);
        }
        return getSigningKeyForKid(cachedJwks.get(publicJwkEndpoint), kid).map(this::toBase64);
    }

    // Remove when the feature flag is permanent
    public boolean isUsingPublicJwk() {
        return usePublicJwk;
    }

    private Optional<Key> getSigningKeyForKid(JWKS jwks, String kid) {
        if (jwks == null || jwks.getKeys() == null) {
            return Optional.empty();
        }
        return jwks.getKeys().stream()
                .filter(entry -> entry.getUse().equals("sig") && entry.getKid().equals(kid))
                .findFirst();
    }

    private String toBase64(Key key) {
        try {
            String str = OBJECT_MAPPER.writeValueAsString(key);
            return Base64.getEncoder().encodeToString(str.getBytes());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
