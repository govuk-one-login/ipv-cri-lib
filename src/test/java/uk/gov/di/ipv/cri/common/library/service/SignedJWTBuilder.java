package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ResponseType;
import software.amazon.awssdk.utils.StringUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

class SignedJWTBuilder {

    private static final String SHARED_CLAIMS =
            "{\"@context\":[\"https:\\/\\/www.w3.org\\/2018\\/credentials\\/v1\",\"https:\\/\\/vocab.london.cloudapps.digital\\/contexts\\/identity-v1.jsonld\"],\"name\":[{\"nameParts\":[{\"type\":\"GivenName\",\"value\":\"KENNETH\"},{\"type\":\"FamilyName\",\"value\":\"DECERQUEIRA\"}]}],\"birthDate\":[{\"value\":\"1965-04-05\"}],\"address\":[{\"buildingNumber\":\"8\",\"streetName\":\"HADLEY ROAD\",\"postalCode\":\"BA2 5AA\",\"validFrom\":\"2021-01-01\"}]}";
    private String issuer = "ipv-core";
    private Instant now = Instant.now();
    private JWSAlgorithm signingAlgorithm = JWSAlgorithm.RS256;
    private Certificate certificate = null;
    private String redirectUri = "https://www.example/com/callback";
    private String clientId = "ipv-core";
    private Date notBeforeTime = Date.from(now);
    private String audience = "test-audience";
    private boolean includeSubject = true;
    private boolean includeSharedClaims = false;
    private String persistentSessionId = "persistentSessionIdTest";
    private String clientSessionId = "clientSessionIdTest";
    private Map<String, Object> sharedClaims = null;
    private String context;
    private KeyPair keyPair;

    SignedJWTBuilder setNow(Instant now) {
        this.now = now;
        return this;
    }

    SignedJWTBuilder setSigningAlgorithm(JWSAlgorithm signingAlgorithm) {
        this.signingAlgorithm = signingAlgorithm;
        return this;
    }

    SignedJWTBuilder setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    SignedJWTBuilder setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    SignedJWTBuilder setNotBeforeTime(Date notBeforeTime) {
        this.notBeforeTime = notBeforeTime;
        return this;
    }

    SignedJWTBuilder setAudience(String audience) {
        this.audience = audience;
        return this;
    }

    SignedJWTBuilder setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    SignedJWTBuilder setIncludeSubject(boolean includeSubject) {
        this.includeSubject = includeSubject;
        return this;
    }

    SignedJWTBuilder setIncludeSharedClaims(boolean includeSharedClaims) {
        this.includeSharedClaims = includeSharedClaims;
        return this;
    }

    SignedJWTBuilder setSharedClaims(Map<String, Object> sharedClaims) {
        this.sharedClaims = sharedClaims;
        return this;
    }

    SignedJWTBuilder setPersistentSessionId(String persistentSessionId) {
        this.persistentSessionId = persistentSessionId;
        return this;
    }

    SignedJWTBuilder setClientSessionId(String clientSessionId) {
        this.clientSessionId = clientSessionId;
        return this;
    }

    SignedJWTBuilder setContext(String context) {
        this.context = context;
        return this;
    }

    Certificate getCertificate() {
        return certificate;
    }

    SignedJWT build() {
        try {

            keyPair = generateKeyPair();
            String kid = UUID.randomUUID().toString();
            String ipvSessionId = UUID.randomUUID().toString();

            JWTClaimsSet.Builder jwtClaimSetBuilder =
                    new JWTClaimsSet.Builder()
                            .audience(audience)
                            .issueTime(Date.from(now))
                            .issuer(issuer)
                            .notBeforeTime(notBeforeTime)
                            .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                            .claim("claims", Map.of("vc_http_api", Map.of()))
                            .claim(
                                    "response_type",
                                    ResponseType.CODE.stream().findFirst().get().getValue())
                            .claim("client_id", clientId)
                            .claim("redirect_uri", redirectUri)
                            .claim("state", "state")
                            .claim("persistent_session_id", persistentSessionId)
                            .claim("govuk_signin_journey_id", clientSessionId);

            if (includeSubject) {
                jwtClaimSetBuilder.subject(ipvSessionId);
            }
            if (Objects.isNull(sharedClaims) && includeSharedClaims) {
                jwtClaimSetBuilder.claim(
                        "shared_claims", new ObjectMapper().readValue(SHARED_CLAIMS, Map.class));

            } else {
                if (Objects.nonNull(sharedClaims)) {
                    jwtClaimSetBuilder.claim("shared_claims", this.sharedClaims);
                }
            }

            if (!StringUtils.isEmpty(context)) {
                jwtClaimSetBuilder.claim("context", this.context);
            }

            SignedJWT signedJWT =
                    new SignedJWT(
                            new JWSHeader.Builder(signingAlgorithm).keyID(kid).build(),
                            jwtClaimSetBuilder.build());

            PrivateKey privateKey = keyPair.getPrivate();
            if (privateKey instanceof RSAPrivateKey) {
                signedJWT.sign(new RSASSASigner((RSAPrivateKey) privateKey));
            } else if (privateKey instanceof ECPrivateKey) {
                signedJWT.sign(new ECDSASigner((ECPrivateKey) privateKey));
            } else {
                throw new IllegalStateException("Unsupported private key type");
            }
            return signedJWT;
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator;
            if (signingAlgorithm.getName().startsWith("RS")) {
                keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(2048);
            } else if (signingAlgorithm.getName().startsWith("ES")) {
                keyPairGenerator = KeyPairGenerator.getInstance("EC");
                keyPairGenerator.initialize(256);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported signing algorithm: " + signingAlgorithm);
            }
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Key generation error", e);
        }
    }
}
