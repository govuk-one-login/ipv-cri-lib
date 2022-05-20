package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

import static com.nimbusds.jose.JWSAlgorithm.ES256;

public class KMSSigner implements JWSSigner {

    private static final Base64.Encoder b64UrlEncoder = Base64.getUrlEncoder();
    private final KmsClient kmsClient;
    private final JCAContext jcaContext = new JCAContext();
    private final String keyId;

    @ExcludeFromGeneratedCoverageReport
    public KMSSigner(String keyId) {
        this.keyId = keyId;
        this.kmsClient = KmsClient.builder().build();
    }

    public KMSSigner(String keyId, KmsClient kmsClient) {
        this.keyId = keyId;
        this.kmsClient = kmsClient;
    }

    @Override
    public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
        Objects.requireNonNull(signingInput, "Signing input must not be null");

        byte[] signingInputHash;

        try {
            signingInputHash = MessageDigest.getInstance("SHA-256").digest(signingInput);
        } catch (NoSuchAlgorithmException e) {
            throw new JOSEException(e.getMessage());
        }

        SignRequest signRequest =
                SignRequest.builder()
                        .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256.toString())
                        .keyId(keyId)
                        .message(SdkBytes.fromByteArray(signingInputHash))
                        .messageType(MessageType.DIGEST)
                        .build();

        SignResponse signResponse = kmsClient.sign(signRequest);

        return new Base64URL(b64UrlEncoder.encodeToString(signResponse.signature().asByteArray()));
    }

    @Override
    public Set<JWSAlgorithm> supportedJWSAlgorithms() {
        return Set.of(ES256);
    }

    @Override
    public JCAContext getJCAContext() {
        return jcaContext;
    }
}
