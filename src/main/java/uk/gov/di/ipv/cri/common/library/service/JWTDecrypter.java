package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

public class JWTDecrypter {
    private final KMSRSADecrypter decrypter;

    public JWTDecrypter(KMSRSADecrypter decrypter) {
        this.decrypter = decrypter;
    }

    public SignedJWT decrypt(String serialisedJweObj) throws ParseException, JOSEException {
        JWEObject requestJweObj = JWEObject.parse(serialisedJweObj);
        requestJweObj.decrypt(this.decrypter);
        return requestJweObj.getPayload().toSignedJWT();
    }
}
