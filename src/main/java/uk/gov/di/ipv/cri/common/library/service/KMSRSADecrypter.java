package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage;
import com.nimbusds.jose.crypto.impl.ContentCryptoProvider;
import com.nimbusds.jose.jca.JWEJCAContext;
import com.nimbusds.jose.util.Base64URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.EncryptionAlgorithmSpec;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Objects;
import java.util.Set;

import static software.amazon.awssdk.services.kms.model.EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256;

public class KMSRSADecrypter implements JWEDecrypter {
    private static final Set<JWEAlgorithm> SUPPORTED_ALGORITHMS = Set.of(JWEAlgorithm.RSA_OAEP_256);
    private static final Set<EncryptionMethod> SUPPORTED_ENCRYPTION_METHODS =
            Set.of(EncryptionMethod.A256GCM);
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SESSION_DECRYPTION_KEY_PRIMARY_ALIAS =
            "session_decryption_key_active_alias";
    private static final String SESSION_DECRYPTION_KEY_SECONDARY_ALIAS =
            "session_decryption_key_inactive_alias";
    private static final String SESSION_DECRYPTION_KEY_PREVIOUS_ALIAS =
            "session_decryption_key_previous_alias";
    private static final String ALL_ALIASES_UNAVAILABLE = "all_aliases_unavailable_for_decryption";
    private boolean keyRotationEnabled = false;
    private boolean keyRotationLegacyKeyFallbackEnabled = false;
    private final JWEJCAContext jcaContext;
    private final KmsClient kmsClient;
    private final EventProbe eventProbe;
    private final String keyId;

    public KMSRSADecrypter(String keyId, KmsClient kmsClient, EventProbe eventProbe) {
        this(
                kmsClient,
                eventProbe,
                keyId,
                Boolean.parseBoolean(System.getenv("ENV_VAR_FEATURE_FLAG_KEY_ROTATION")),
                Boolean.parseBoolean(
                        System.getenv("ENV_VAR_FEATURE_FLAG_KEY_ROTATION_LEGACY_KEY_FALLBACK")));
    }

    public KMSRSADecrypter(
            KmsClient kmsClient,
            EventProbe eventProbe,
            String keyId,
            Boolean keyRotationEnabled,
            boolean legacyKeyFallbackEnabled) {
        this.jcaContext = new JWEJCAContext();
        this.kmsClient = kmsClient;
        this.eventProbe = eventProbe;
        this.keyId = keyId;
        this.keyRotationEnabled = keyRotationEnabled;
        this.keyRotationLegacyKeyFallbackEnabled = legacyKeyFallbackEnabled;
    }

    @Override
    public Set<JWEAlgorithm> supportedJWEAlgorithms() {
        return SUPPORTED_ALGORITHMS;
    }

    @Override
    public Set<EncryptionMethod> supportedEncryptionMethods() {
        return SUPPORTED_ENCRYPTION_METHODS;
    }

    @Override
    public JWEJCAContext getJCAContext() {
        return jcaContext;
    }

    public boolean isKeyRotationEnabled() {
        return keyRotationEnabled;
    }

    @Override
    public byte[] decrypt(
            JWEHeader header,
            Base64URL encryptedKey,
            Base64URL iv,
            Base64URL cipherText,
            Base64URL authTag,
            byte[] aad)
            throws JOSEException {

        // Validate required JWE parts
        validateJwe(header, encryptedKey, iv, authTag);

        DecryptResponse decryptResponse;
        if (keyRotationEnabled) {
            LOGGER.info("Key rotation enabled. Attempting to decrypt with key aliases.");
            // During a key rotation, we might receive JWTs encrypted with either the old or new
            // key.
            decryptResponse = decryptWithKeyAliases(encryptedKey);

            if (keyRotationLegacyKeyFallbackEnabled && decryptResponse == null) {
                LOGGER.warn(
                        "Failed to decrypt with all available key aliases, falling back to legacy key.");

                // Legacy Key fallback
                try {
                    decryptResponse = decryptWithLegacyKey(encryptedKey);
                } catch (Exception e) {
                    // Do nothing
                }

                if (decryptResponse == null) {
                    String message = "Failed to decrypt with legacy key.";
                    LOGGER.error(message);
                    throw new JOSEException(message);
                }

                LOGGER.info("Decryption successful with legacy key");
            } else if (decryptResponse == null) {
                String message = "Failed to decrypt with all available key aliases.";
                LOGGER.error(message);
                throw new JOSEException(message);
            }
        } else {
            // Legacy Key Route
            decryptResponse = decryptWithLegacyKey(encryptedKey);
        }
        SecretKey cek = new SecretKeySpec(decryptResponse.plaintext().asByteArray(), "AES");
        return ContentCryptoProvider.decrypt(
                header, null, encryptedKey, iv, cipherText, authTag, cek, getJCAContext());
    }

    private void validateJwe(
            JWEHeader header, Base64URL encryptedKey, Base64URL iv, Base64URL authTag)
            throws JOSEException {
        if (Objects.isNull(encryptedKey)) {
            throw new JOSEException("Missing JWE encrypted key");
        }

        if (Objects.isNull(iv)) {
            throw new JOSEException("Missing JWE initialization vector (IV)");
        }

        if (Objects.isNull(authTag)) {
            throw new JOSEException("Missing JWE authentication tag");
        }

        JWEAlgorithm alg = header.getAlgorithm();

        if (!SUPPORTED_ALGORITHMS.contains(alg)) {
            throw new JOSEException(
                    AlgorithmSupportMessage.unsupportedJWEAlgorithm(alg, supportedJWEAlgorithms()));
        }
    }

    private DecryptResponse decryptWithLegacyKey(Base64URL encryptedKey) {
        DecryptRequest decryptRequest =
                DecryptRequest.builder()
                        .encryptionAlgorithm(EncryptionAlgorithmSpec.RSAES_OAEP_SHA_256)
                        .ciphertextBlob(SdkBytes.fromByteArray(encryptedKey.decode()))
                        .keyId(this.keyId)
                        .build();
        return this.kmsClient.decrypt(decryptRequest);
    }

    private DecryptResponse decryptWithKeyAliases(Base64URL encryptedKey) {
        String[] keyAliases = {
            SESSION_DECRYPTION_KEY_PRIMARY_ALIAS,
            SESSION_DECRYPTION_KEY_SECONDARY_ALIAS,
            SESSION_DECRYPTION_KEY_PREVIOUS_ALIAS
        };

        DecryptResponse decryptResponse = null;
        for (String alias : keyAliases) {
            try {
                decryptResponse = kmsClient.decrypt(buildDecryptRequest(alias, encryptedKey));
                LOGGER.info("Decryption successful with key alias: {}", alias);
                return decryptResponse;
            } catch (Exception e) {
                LOGGER.warn(
                        "Failed to decrypt with key alias: {}. Error: {}", alias, e.getMessage());
            }
        }

        eventProbe.counterMetric(ALL_ALIASES_UNAVAILABLE);

        return decryptResponse;
    }

    private DecryptRequest buildDecryptRequest(String keyAlias, Base64URL encryptedKey) {
        return DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(encryptedKey.decode()))
                .encryptionAlgorithm(RSAES_OAEP_SHA_256)
                .keyId("alias/" + keyAlias)
                .build();
    }
}
