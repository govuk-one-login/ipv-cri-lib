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
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

public class StubClient {
    SsmClient ssmClient;

    public StubClient() {

        ssmClient = new ClientProviderFactory().getSsmClient();
    }

    private static final long hourTimeLimit = 3600_000;
    private static final String issuer = "ipv-core-stub-aws-headless";

    public PrivateKeyJWT generateClientAssertion(String audience)
            throws JOSEException, ParseException {
        var claimsSetValues =
                new JWTClaimsSet.Builder()
                        .claim(JWTClaimNames.ISSUER, issuer)
                        .claim(JWTClaimNames.SUBJECT, issuer)
                        .claim(JWTClaimNames.AUDIENCE, audience)
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
        return ECKey.parse(getParameterValueByAbsoluteName());
    }

    private String getParameterValueByAbsoluteName() {
        try {
            GetParameterRequest parameterRequest =
                    GetParameterRequest.builder()
                            .name("/test-resources/ipv-core-stub-aws-headless/privateSigningKey")
                            .build();

            GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            return parameterResponse.parameter().value();
        } catch (SsmException e) {
            e.getMessage();
            throw e;
        }
    }
}
