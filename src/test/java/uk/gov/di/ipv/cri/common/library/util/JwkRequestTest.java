package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.jwks.JWKS;
import uk.gov.di.ipv.cri.common.library.domain.jwks.Key;
import uk.gov.di.ipv.cri.common.library.exception.JWKSRequestException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwkRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock private HttpClient mockHttpClient;
    @Mock private HttpResponse<Object> mockHttpResponse;
    @Mock private HttpHeaders mockHttpHeaders;

    private static final String MOCK_API_RESPONSE =
            "{\n"
                    + "  \"keys\": [\n"
                    + "    {\n"
                    + "      \"kty\": \"RSA\",\n"
                    + "      \"e\": \"AQAB\",\n"
                    + "      \"use\": \"enc\",\n"
                    + "      \"alg\": \"RS256\",\n"
                    + "      \"n\": \"dummy-n\",\n"
                    + "      \"kid\": \"dummy-kid\"\n"
                    + "    },\n"
                    + "    {\n"
                    + "      \"kty\": \"EC\",\n"
                    + "      \"use\": \"sig\",\n"
                    + "      \"crv\": \"P-256\",\n"
                    + "      \"x\": \"dummy-x\",\n"
                    + "      \"y\": \"dummy-y\",\n"
                    + "      \"alg\": \"ES256\",\n"
                    + "      \"kid\": \"dummy-kid\"\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}";

    @Test
    void shouldReturnCacheControlHeader() throws Exception {
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.body()).thenReturn(MOCK_API_RESPONSE);
        when(mockHttpResponse.headers()).thenReturn(mockHttpHeaders);
        when(mockHttpHeaders.firstValue("Cache-Control")).thenReturn(Optional.of("max-age=300"));

        JwkRequest request = new JwkRequest(mockHttpClient, objectMapper);
        JWKS jwks = request.callJWKSEndpoint("https://example.com/.well-known/jwks.json");

        verify(mockHttpClient).send(any(), any());
        verify(mockHttpResponse).body();
        verify(mockHttpResponse).headers();
        verify(mockHttpHeaders).firstValue("Cache-Control");

        assertEquals(300, jwks.getMaxAgeFromCacheControlHeader());
    }

    @Test
    void cacheControlHeaderZero() throws Exception {
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.body()).thenReturn(MOCK_API_RESPONSE);
        when(mockHttpResponse.headers()).thenReturn(mockHttpHeaders);
        when(mockHttpHeaders.firstValue("Cache-Control")).thenReturn(Optional.of("invalid"));

        JwkRequest request = new JwkRequest(mockHttpClient, objectMapper);
        JWKS jwks = request.callJWKSEndpoint("https://example.com/.well-known/jwks.json");

        verify(mockHttpClient).send(any(), any());
        verify(mockHttpResponse).body();
        verify(mockHttpResponse).headers();
        verify(mockHttpHeaders).firstValue("Cache-Control");

        assertEquals(0, jwks.getMaxAgeFromCacheControlHeader());
    }

    @Test
    void shouldReturnKeys() throws Exception {
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.body()).thenReturn(MOCK_API_RESPONSE);
        when(mockHttpResponse.headers()).thenReturn(mockHttpHeaders);

        JwkRequest request = new JwkRequest(mockHttpClient, objectMapper);
        JWKS jwks = request.callJWKSEndpoint("https://example.com/.well-known/jwks.json");

        List<Key> keys = jwks.getKeys();

        verify(mockHttpClient).send(any(), any());
        verify(mockHttpResponse).body();
        verify(mockHttpResponse).headers();

        assertEquals(2, keys.size());
        assertEquals("RSA", keys.get(0).getKty());
        assertEquals("AQAB", keys.get(0).getE());
        assertEquals("enc", keys.get(0).getUse());
        assertEquals("RS256", keys.get(0).getAlg());
        assertEquals("dummy-n", keys.get(0).getN());
        assertEquals("dummy-kid", keys.get(0).getKid());
        assertNull(keys.get(0).getX());

        assertEquals("EC", keys.get(1).getKty());
        assertEquals("sig", keys.get(1).getUse());
        assertEquals("P-256", keys.get(1).getCrv());
        assertEquals("dummy-x", keys.get(1).getX());
        assertEquals("dummy-y", keys.get(1).getY());
        assertEquals("ES256", keys.get(1).getAlg());
        assertEquals("dummy-kid", keys.get(1).getKid());
    }

    @Test
    void showThrowNPEWhenInvalidURL() {
        JwkRequest request = new JwkRequest(HttpClient.newHttpClient(), objectMapper);
        assertThrows(JWKSRequestException.class, () -> request.callJWKSEndpoint("not-valid-url"));
    }

    @Test
    void showThrowErrorWhenInvalidBody() throws IOException, InterruptedException {
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.body()).thenReturn("invalid-response");

        JwkRequest request = new JwkRequest(mockHttpClient, objectMapper);

        assertThrows(
                JWKSRequestException.class,
                () -> request.callJWKSEndpoint("https://example.com/.well-known/jwks.json"));
    }
}
