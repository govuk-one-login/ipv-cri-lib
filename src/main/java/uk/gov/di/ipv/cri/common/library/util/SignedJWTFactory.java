package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

public class SignedJWTFactory {

    private static final String KID_PREFIX = "did:web:";
    private final JWSSigner kmsSigner;

    public SignedJWTFactory(JWSSigner kmsSigner) {
        this.kmsSigner = kmsSigner;
    }

    // Updated method for passing issuer used in building kid
    public SignedJWT createSignedJwt(JWTClaimsSet claimsSet, String issuer, String keyId)
            throws JOSEException, NoSuchAlgorithmException {
        JWSHeader jwsHeader = generateHeader(issuer, keyId);
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        signedJWT.sign(kmsSigner);
        return signedJWT;
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

    // Updated method for passing issuer used in building kid
    private JWSHeader generateHeader(String issuer, String signingKeyId)
            throws NoSuchAlgorithmException {

        issuer = issuer.replaceFirst("https://", "");

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(signingKeyId.getBytes(StandardCharsets.UTF_8));
        String hashedKeyId = byteArrayToHex(hash);

        String keyId = KID_PREFIX + issuer + "#" + hashedKeyId;

        return new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(JOSEObjectType.JWT)
                .keyID(keyId)
                .build();
    }

    private static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
