package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SignedJWTFactoryTest {
    private static final String EC_PRIVATE_KEY_1 =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgOXt0P05ZsQcK7eYusgIPsqZdaBCIJiW4imwUtnaAthWhRANCAAQT1nO46ipxVTilUH2umZPN7OPI49GU6Y8YkcqLxFKUgypUzGbYR2VJGM+QJXk0PI339EyYkt6tjgfS+RcOMQNO";
    private static final String EC_PUBLIC_JWK_1 =
            "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"E9ZzuOoqcVU4pVB9rpmTzezjyOPRlOmPGJHKi8RSlIM\",\"y\":\"KlTMZthHZUkYz5AleTQ8jff0TJiS3q2OB9L5Fw4xA04\"}";
    private SignedJWTFactory signedJwtFactory;

    @Test
    void shouldCreateASignedJwtSuccessfullyFromJWTClaimsSet()
            throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException,
                    ParseException {
        JWTClaimsSet testClaimsSet = new JWTClaimsSet.Builder().build();
        signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));

        SignedJWT signedJWT = signedJwtFactory.createSignedJwt(testClaimsSet);

        assertThat(signedJWT.verify(new ECDSAVerifier(ECKey.parse(EC_PUBLIC_JWK_1))), is(true));
    }

    @Test
    void shouldCreateASignedJwtSuccessfullyFromJWTClaimsSetString()
            throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException,
                    ParseException {
        signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));

        SignedJWT signedJWT =
                signedJwtFactory.createSignedJwt(
                        "{\"iss\":\"dummyAddressComponentId\",\"sub\":\"test-subject\",\"nbf\":4070908800,\"exp\":4070909400,\"jti\":\"dummyJti\"}");

        assertThat(signedJWT.verify(new ECDSAVerifier(ECKey.parse(EC_PUBLIC_JWK_1))), is(true));
    }

    @Test
    void shouldCreateASignedJwtSuccessfullyFromJWTClaimsSetWhenKeyIDInHeader()
            throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException,
                    ParseException {
        JWTClaimsSet testClaimsSet = new JWTClaimsSet.Builder().build();
        signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));

        SignedJWT signedJWT = signedJwtFactory.createSignedJwt(testClaimsSet, "issuer", "keyId");

        assertThat(signedJWT.verify(new ECDSAVerifier(ECKey.parse(EC_PUBLIC_JWK_1))), is(true));
    }

    @Test
    void shouldCreateASignedJwtSuccessfullyWithCorrectKeyIdStructure()
            throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException,
                    JsonProcessingException {
        JWTClaimsSet testClaimsSet = new JWTClaimsSet.Builder().build();
        signedJwtFactory = new SignedJWTFactory(new ECDSASigner(getPrivateKey()));

        SignedJWT signedJWT = signedJwtFactory.createSignedJwt(testClaimsSet, "issuer", "keyId");
        System.out.println(signedJWT.getHeader().getKeyID());

        String[] keyIDArray = signedJWT.getHeader().getKeyID().split(":");

        System.out.println(new ObjectMapper().writeValueAsString(signedJWT));
        assertEquals("did", keyIDArray[0]);
        assertEquals("web", keyIDArray[1]);
        assertEquals("issuer", keyIDArray[2]);
    }

    private ECPrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return (ECPrivateKey)
                KeyFactory.getInstance("EC")
                        .generatePrivate(
                                new PKCS8EncodedKeySpec(
                                        Base64.getDecoder().decode(EC_PRIVATE_KEY_1)));
    }
}
