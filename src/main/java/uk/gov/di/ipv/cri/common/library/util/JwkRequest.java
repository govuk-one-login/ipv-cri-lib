package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.domain.jwks.JWKS;
import uk.gov.di.ipv.cri.common.library.exception.JWKSRequestException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class JwkRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwkRequest.class);
    private static final String CACHE_CONTROL_HEADER_NAME = "Cache-Control";
    private static final String MAX_AGE_PREFIX = "max-age=";
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    public JwkRequest() {
        this(null, new ObjectMapper());
    }

    public JwkRequest(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public JWKS callJWKSEndpoint(String endpoint) throws JWKSRequestException {
        LOGGER.info("Calling JWKS endpoint ({})", endpoint);

        HttpRequest request = createRequest(endpoint);
        HttpResponse<String> response = sendRequest(request);

        if (response.statusCode() != 200) {
            throw new JWKSRequestException(
                    "JWK endpoint returned status code " + response.statusCode());
        }

        try {
            JWKS jwks = objectMapper.readValue(response.body(), JWKS.class);
            parseCacheControlHeader(response).ifPresent(jwks::setMaxAgeFromCacheControlHeader);
            return jwks;
        } catch (Exception e) {
            throw new JWKSRequestException("Failed to parse JWKS endpoint response", e);
        }
    }

    private HttpRequest createRequest(String endpoint) throws JWKSRequestException {
        try {
            return HttpRequest.newBuilder().uri(new URI(endpoint)).GET().build();
        } catch (Exception e) {
            throw new JWKSRequestException("Failed to create request for endpoint: " + endpoint, e);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request) throws JWKSRequestException {
        try {
            if (httpClient == null) {
                httpClient = HttpClient.newBuilder().build();
            }
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()); // NOSONAR
        } catch (Exception e) { // NOSONAR
            throw new JWKSRequestException("Failed to send HTTP request", e);
        } finally {
            httpClient = null;
        }
    }

    private Optional<Integer> parseCacheControlHeader(HttpResponse<String> response) {
        return response.headers()
                .firstValue(CACHE_CONTROL_HEADER_NAME)
                .filter(value -> value.startsWith(MAX_AGE_PREFIX))
                .map(
                        value -> {
                            try {
                                return Integer.parseInt(value.substring(MAX_AGE_PREFIX.length()));
                            } catch (NumberFormatException e) {
                                LOGGER.warn(
                                        "Invalid max-age value in Cache-Control header: {}",
                                        value,
                                        e);
                                return 0;
                            }
                        });
    }
}
