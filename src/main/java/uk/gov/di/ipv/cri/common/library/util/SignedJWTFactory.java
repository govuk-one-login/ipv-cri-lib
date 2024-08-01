package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

public class SignedJWTFactory {
    private final JWSSigner kmsSigner;

    public SignedJWTFactory(JWSSigner kmsSigner) {
        this.kmsSigner = kmsSigner;
    }

    public SignedJWT createSignedJwt(JWTClaimsSet claimsSet) throws JOSEException {
        JWSHeader jwsHeader = generateHeader();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(kmsSigner);
        return signedJWT;
    }

    public SignedJWT createSignedJwt(String claimsSet) throws ParseException, JOSEException {

        SignedJWT signedJWT = new SignedJWT(generateHeader(), JWTClaimsSet.parse(claimsSet));
        signedJWT.sign(kmsSigner);
        Base64URL header = generateHeader().toBase64URL();
        Base64URL payload = Base64URL.encode(claimsSet);

        Base64URL signature = signedJWT.getSignature();
        return new SignedJWT(header, payload, signature);
    }

    private JWSHeader generateHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT).build();
    }
}
