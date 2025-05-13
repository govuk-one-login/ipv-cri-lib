package uk.gov.di.ipv.cri.common.library.client;

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
import uk.gov.di.ipv.cri.common.library.helpers.SSMHelper;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

public class StubClient {
    private final ClientConfigurationService clientConfigurationService;
    private final SSMHelper ssmHelper;
    private static final long hourTimeLimit = 3600_000;

    public StubClient(SSMHelper ssmHelper, ClientConfigurationService clientConfigurationService) {
        this.ssmHelper = ssmHelper;
        this.clientConfigurationService = clientConfigurationService;
    }

    public PrivateKeyJWT generateClientAssertion(String clientId)
            throws JOSEException, ParseException {
        var claimsSetValues =
                new JWTClaimsSet.Builder()
                        .claim(JWTClaimNames.ISSUER, clientId)
                        .claim(JWTClaimNames.SUBJECT, clientId)
                        .claim(
                                JWTClaimNames.AUDIENCE,
                                ssmHelper
                                        .getParameterValueByName(
                                                "/common-cri-api/clients/"
                                                        + this.clientConfigurationService
                                                                .getDefaultClientId()
                                                        + "/jwtAuthentication/audience")
                                        .trim())
                        .claim(JWTClaimNames.JWT_ID, UUID.randomUUID())
                        .claim(
                                JWTClaimNames.EXPIRATION_TIME,
                                new Date(new Date().getTime() + hourTimeLimit))
                        .build();

        ECDSASigner signer = new ECDSASigner(getEcPrivateKey());

        JWSHeader jwtHeader =
                new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
        SignedJWT signedJWT = new SignedJWT(jwtHeader, claimsSetValues);
        signedJWT.sign(signer);

        return new PrivateKeyJWT(signedJWT);
    }

    private ECKey getEcPrivateKey() throws ParseException {
        return ECKey.parse(
                ssmHelper
                        .getParameterValueByName(
                                "/test-resources/"
                                        + this.clientConfigurationService.getDefaultClientId()
                                        + "/privateSigningKey")
                        .trim());
    }
}
