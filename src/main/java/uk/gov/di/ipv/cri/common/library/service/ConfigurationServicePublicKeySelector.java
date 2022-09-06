package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientCredentialsSelector;
import com.nimbusds.oauth2.sdk.auth.verifier.Context;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.id.ClientID;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;

public class ConfigurationServicePublicKeySelector implements ClientCredentialsSelector<Object> {

    private final ConfigurationService configurationService;

    public ConfigurationServicePublicKeySelector(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public List<Secret> selectClientSecrets(
            ClientID claimedClientID, ClientAuthenticationMethod authMethod, Context context) {
        throw new UnsupportedOperationException("We don't do that round here...");
    }

    @Override
    public List<? extends PublicKey> selectPublicKeys(
            ClientID claimedClientID,
            ClientAuthenticationMethod authMethod,
            JWSHeader jwsHeader,
            boolean forceRefresh,
            Context context)
            throws InvalidClientException {
        try {
            return List.of(
                    getPublicKeyFromConfig(
                            configurationService
                                    .getParametersForPath(
                                            "/clients/"
                                                    + claimedClientID.getValue()
                                                    + "/jwtAuthentication")
                                    .get("publicSigningJwkBase64"),
                            jwsHeader.getAlgorithm()));
        } catch (ParseException | JOSEException | CertificateException e) {
            throw new InvalidClientException(e.getMessage());
        }
    }

    private PublicKey getPublicKeyFromConfig(
            String serialisedPublicKey, JWSAlgorithm signingAlgorithm)
            throws CertificateException, ParseException, JOSEException {
        if (JWSAlgorithm.Family.RSA.contains(signingAlgorithm)) {
            byte[] binaryCertificate = Base64.getDecoder().decode(serialisedPublicKey);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate certificate =
                    factory.generateCertificate(new ByteArrayInputStream(binaryCertificate));
            return certificate.getPublicKey();
        } else if (JWSAlgorithm.Family.EC.contains(signingAlgorithm)) {
            return ECKey.parse(new String(Base64.getDecoder().decode(serialisedPublicKey)))
                    .toECPublicKey();
        } else {
            throw new IllegalArgumentException(
                    "Unexpected signing algorithm encountered: " + signingAlgorithm.getName());
        }
    }
}
