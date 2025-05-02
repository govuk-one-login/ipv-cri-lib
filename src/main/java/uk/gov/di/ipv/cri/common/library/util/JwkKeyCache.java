package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.domain.jwks.JWKS;
import uk.gov.di.ipv.cri.common.library.domain.jwks.Key;
import uk.gov.di.ipv.cri.common.library.exception.JWKSRequestException;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class JwkKeyCache {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(JwkKeyCache.class);

    private final boolean usePublicJwk;
    private final String publicJwkEndpoint;
    private final JwkRequest jwkRequest;

    private JWKS cachedJwks;
    private long lastUpdated;
    private long cacheControl;

    public JwkKeyCache() {
        this(new JwkRequest());
    }

    public JwkKeyCache(JwkRequest jwkRequest) {
        this.jwkRequest = jwkRequest;
        this.lastUpdated = 0;
        this.cacheControl = 0;
        usePublicJwk =
                Boolean.parseBoolean(
                        Optional.ofNullable(System.getenv("ENV_VAR_FEATURE_CONSUME_PUBLIC_JWK"))
                                .orElse("false"));
        publicJwkEndpoint = System.getenv("PUBLIC_JWKS_ENDPOINT");
    }

    public JwkKeyCache(JwkRequest jwkRequest, boolean usePublicJwk, String publicJwkEndpoint) {
        this.jwkRequest = jwkRequest;
        this.lastUpdated = 0;
        this.cacheControl = 0;
        this.usePublicJwk = usePublicJwk;
        this.publicJwkEndpoint = publicJwkEndpoint;
    }

    public Optional<String> getBase64JwkForKid(String kid) {
        if (!usePublicJwk || publicJwkEndpoint == null) {
            LOGGER.info("Using public JWKs endpoint is disabled");
            return Optional.empty();
        }
        LOGGER.info("Using JWKs endpoint: {}", publicJwkEndpoint);
        if (cachedJwks == null || System.currentTimeMillis() > lastUpdated + cacheControl) {
            try {
                cachedJwks = jwkRequest.callJWKSEndpoint(publicJwkEndpoint);
            } catch (JWKSRequestException e) {
                LOGGER.error("Failed to call JWK endpoint ({})", publicJwkEndpoint, e);
                return Optional.empty();
            }
            lastUpdated = System.currentTimeMillis();
            cacheControl = TimeUnit.SECONDS.toMillis(cachedJwks.getMaxAgeFromCacheControlHeader());
            LOGGER.info(
                    "JWKs cache has been updated to '{}' seconds",
                    cachedJwks.getMaxAgeFromCacheControlHeader());
        } else {
            LOGGER.info("Using locally cached JWKs from {}", publicJwkEndpoint);
        }
        return getSigningKeyForKid(cachedJwks, kid).map(this::toBase64);
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
