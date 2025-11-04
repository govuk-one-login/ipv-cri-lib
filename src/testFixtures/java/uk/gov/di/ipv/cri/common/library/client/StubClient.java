package uk.gov.di.ipv.cri.common.library.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import uk.gov.di.ipv.cri.common.library.config.CriStubClientEnum;
import uk.gov.di.ipv.cri.common.library.helpers.SSMHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

public class StubClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final ClientConfigurationService clientConfigurationService;
    private static final long HOUR_TIME_LIMIT = 3_600_000;
    private final SSMHelper ssmHelper;

    public StubClient(SSMHelper ssmHelper, ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;
        this.ssmHelper = ssmHelper;
    }

    public PrivateKeyJWT generateClientAssertion(String clientId)
            throws JOSEException, ParseException {
        var claimsSetValues =
                new JWTClaimsSet.Builder()
                        .claim(JWTClaimNames.ISSUER, clientId)
                        .claim(JWTClaimNames.SUBJECT, clientId)
                        .claim(
                                JWTClaimNames.AUDIENCE,
                                ssmHelper.getParameterValue(CriStubClientEnum.AUDIENCE))
                        .claim(JWTClaimNames.JWT_ID, UUID.randomUUID())
                        .claim(
                                JWTClaimNames.EXPIRATION_TIME,
                                new Date(new Date().getTime() + HOUR_TIME_LIMIT))
                        .build();

        String privateSigningKey = getPrivateSigningKey();
        String publicKeyId = sha256(getKidFromPrivateSigningKey(privateSigningKey));

        ECDSASigner signer = new ECDSASigner(getEcPrivateKey(privateSigningKey));
        JWSHeader jwtHeader =
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .type(JOSEObjectType.JWT)
                        .keyID(publicKeyId)
                        .build();
        SignedJWT signedJWT = new SignedJWT(jwtHeader, claimsSetValues);
        signedJWT.sign(signer);

        return new PrivateKeyJWT(signedJWT);
    }

    private ECKey getEcPrivateKey(String privateSigningKey) throws ParseException {
        return ECKey.parse(privateSigningKey);
    }

    private String getPrivateSigningKey() {
        return ssmHelper.getParameterValue(
                String.format(
                        "/%s/%s/privateSigningKey",
                        this.clientConfigurationService.getTestResourcesStackName(),
                        this.clientConfigurationService.getDefaultClientId()));
    }

    private String getKidFromPrivateSigningKey(String privateSigningKey) {
        try {
            return OBJECT_MAPPER.readTree(privateSigningKey).get("kid").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
