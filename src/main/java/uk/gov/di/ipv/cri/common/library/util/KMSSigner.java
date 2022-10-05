package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Set;

import static com.nimbusds.jose.JWSAlgorithm.ES256;

public class KMSSigner implements JWSSigner {

    private final KmsClient kmsClient;
    private final JCAContext jcaContext = new JCAContext();
    private final String keyId;

    @ExcludeFromGeneratedCoverageReport
    public KMSSigner(String keyId) {
        this.keyId = keyId;
        this.kmsClient =
                KmsClient.builder()
                        .region(Region.of(System.getenv("AWS_REGION")))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .build();
    }

    public KMSSigner(String keyId, KmsClient kmsClient) {
        this.keyId = keyId;
        this.kmsClient = kmsClient;
    }

    @Override
    public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
        Objects.requireNonNull(signingInput, "Signing input must not be null");

        SignResponse signResponse =
                kmsClient.sign(
                        SignRequest.builder()
                                .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256.toString())
                                .keyId(keyId)
                                .message(SdkBytes.fromByteArray(getSigningInputBytes(signingInput)))
                                .messageType(MessageType.DIGEST)
                                .build());

        byte[] concatSignature =
                ECDSA.transcodeSignatureToConcat(
                        signResponse.signature().asByteArray(),
                        ECDSA.getSignatureByteArrayLength(ES256));

        return Base64URL.encode(concatSignature);
    }

    private byte[] getSigningInputBytes(byte[] signingInput) throws JOSEException {
        byte[] signingInputHash;
        try {
            signingInputHash = MessageDigest.getInstance("SHA-256").digest(signingInput);
        } catch (NoSuchAlgorithmException e) {
            throw new JOSEException(e.getMessage());
        }
        return signingInputHash;
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
