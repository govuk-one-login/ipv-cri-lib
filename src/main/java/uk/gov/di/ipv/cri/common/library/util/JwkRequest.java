package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.di.ipv.cri.common.library.domain.jwks.JWKS;
import uk.gov.di.ipv.cri.common.library.exception.JWKSRequestException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class JwkRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwkRequest.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JwkRequest() {
        this(HttpClient.newHttpClient(), new ObjectMapper());
    }

    public JwkRequest(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public JWKS callJWKSEndpoint(String endpoint) throws JWKSRequestException {
        try {
            HttpRequest request = createRequest(endpoint);
            HttpResponse<String> response = sendRequest(request);
            JWKS jwks = objectMapper.readValue(response.body(), JWKS.class);
            parseCacheControlHeader(response).ifPresent(jwks::setMaxAgeFromCacheControlHeader);
            return jwks;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JWKSRequestException("Failed to retrieve JWKS from endpoint: " + endpoint, e);
        }
    }

    private HttpRequest createRequest(String endpoint) throws JWKSRequestException {
        try {
            return HttpRequest.newBuilder().uri(new URI(endpoint)).GET().build();
        } catch (Exception e) {
            throw new JWKSRequestException("Failed to create request for endpoint: " + endpoint, e);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Optional<Integer> parseCacheControlHeader(HttpResponse<String> response) {
        return response.headers()
                .firstValue("Cache-Control")
                .filter(value -> value.startsWith("max-age="))
                .map(
                        value -> {
                            try {
                                return Integer.parseInt(value.substring("max-age=".length()));
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
