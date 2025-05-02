package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.jwks.JWKS;
import uk.gov.di.ipv.cri.common.library.domain.jwks.Key;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwkKeyCacheTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private JwkRequest mockJwkRequest;

    @Test
    void shouldFetchJwk() throws Exception {
        String kid = "dummyKid";

        JWKS jwks = new JWKS();
        jwks.setMaxAgeFromCacheControlHeader(300);

        Key key = new Key();
        key.setUse("sig");
        key.setKid(kid);

        jwks.setKeys(List.of(key));

        when(mockJwkRequest.callJWKSEndpoint(anyString())).thenReturn(jwks);

        JwkKeyCache jwkKeyCache = new JwkKeyCache(mockJwkRequest, true, "https://example.com");
        Optional<String> jwk = jwkKeyCache.getBase64JwkForKid(kid);

        String base64Key =
                Base64.getEncoder()
                        .encodeToString(OBJECT_MAPPER.writeValueAsString(key).getBytes());

        assertTrue(jwk.isPresent());
        assertEquals(base64Key, jwk.get());
    }

    @Test
    void shouldReturnEmptyWhenNullJwk() throws Exception {
        String kid = "dummyKid";

        JWKS jwks = new JWKS();
        jwks.setMaxAgeFromCacheControlHeader(300);

        when(mockJwkRequest.callJWKSEndpoint(anyString())).thenReturn(jwks);

        JwkKeyCache jwkKeyCache = new JwkKeyCache(mockJwkRequest, true, "https://example.com");
        Optional<String> jwk = jwkKeyCache.getBase64JwkForKid(kid);

        assertTrue(jwk.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenDisabled() {
        assertTrue(
                new JwkKeyCache(mockJwkRequest, false, "").getBase64JwkForKid("dummy").isEmpty());
    }
}
