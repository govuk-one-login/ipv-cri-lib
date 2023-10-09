package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.util.Base64URL;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SignResponse;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.nimbusds.jose.JWSAlgorithm.ES256;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KMSSignerTest {
    private KMSSigner kmsSigner;
    private String kid = UUID.randomUUID().toString();
    @Mock private KmsClient mockKmsClient;

    @BeforeEach
    void setUp() {
        kmsSigner = new KMSSigner(kid, mockKmsClient);
    }

    @Test
    void shouldCreateKMSSignerSuccessfully() {
        assertThat(kmsSigner, notNullValue());
        assertThat(kmsSigner.getJCAContext(), notNullValue());
        assertThat(Set.of(ES256), equalTo(kmsSigner.supportedJWSAlgorithms()));
    }

    @Test
    void shouldSignJWSHeaderSuccessfully()
            throws JOSEException, NoSuchAlgorithmException, SignatureException,
                    InvalidKeyException {
        JWSHeader mockJWSHeader = mock(JWSHeader.class);
        byte[] payload = "test payload".getBytes();
        ArgumentCaptor<SignRequest> signRequestArgumentCaptor =
                ArgumentCaptor.forClass(SignRequest.class);

        SignResponse mockSignResponse = mock(SignResponse.class);
        SdkBytes mockSdkBytes = mock(SdkBytes.class);

        when(mockKmsClient.sign(signRequestArgumentCaptor.capture())).thenReturn(mockSignResponse);
        when(mockSignResponse.signature()).thenReturn(mockSdkBytes);
        when(mockSdkBytes.asByteArray()).thenReturn(getDERPayloadBytes(payload));

        var signed = kmsSigner.sign(mockJWSHeader, payload);

        verify(mockKmsClient).sign(signRequestArgumentCaptor.capture());
        SignRequest capturedSignRequest = signRequestArgumentCaptor.getValue();
        assertThat(signed, notNullValue());
        assertThat(capturedSignRequest.keyId(), equalTo(kid));
        assertThat(
                capturedSignRequest.messageTypeAsString(), equalTo(MessageType.DIGEST.toString()));
        assertThat(
                capturedSignRequest.signingAlgorithmAsString(),
                equalTo(SigningAlgorithmSpec.ECDSA_SHA_256.toString()));
    }

    @Test
    void shouldSignJWSObject()
            throws JOSEException, NoSuchAlgorithmException, SignatureException,
                    InvalidKeyException {
        var signResponse = mock(SignResponse.class);
        when(mockKmsClient.sign(any(SignRequest.class))).thenReturn(signResponse);
        when(signResponse.signature())
                .thenReturn(SdkBytes.fromByteArray(getDERPayloadBytes("test payload".getBytes())));

        JSONObject jsonPayload = new JSONObject(Map.of("test", "test"));

        JWSHeader jwsHeader = new JWSHeader.Builder(ES256).build();
        JWSObject jwsObject = new JWSObject(jwsHeader, new Payload(jsonPayload));

        jwsObject.sign(kmsSigner);

        assertEquals(JWSObject.State.SIGNED, jwsObject.getState());
        assertEquals(jwsHeader, jwsObject.getHeader());
        assertEquals(jsonPayload.toJSONString(), jwsObject.getPayload().toString());
    }

    @Test
    void shouldThrowNoSuchAlgorithmExceptionWhenSigningJWSHeaderWithADifferentAlgorithm() {
        JWSHeader mockJWSHeader = mock(JWSHeader.class);

        var exception =
                assertThrows(NullPointerException.class, () -> kmsSigner.sign(mockJWSHeader, null));

        assertThat(exception.getMessage(), containsString("Signing input must not be null"));
    }

    @Test
    void base64UrlSignatureShouldNotIncludePadding() throws Exception {
        var jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256).build();
        var signResponse = mock(SignResponse.class);
        var payload = Base64.getDecoder().decode("dGVzdCBpbnB1dA==");
        byte[] base64PaddedBytes = getDERPayloadBytes(payload);

        when(mockKmsClient.sign(any(SignRequest.class))).thenReturn(signResponse);
        when(signResponse.signature()).thenReturn(SdkBytes.fromByteArray(base64PaddedBytes));

        Base64URL signature = kmsSigner.sign(jwsHeader, payload);
        String signatureString = signature.toString();

        assertThat(Base64.getUrlEncoder().encodeToString(payload).endsWith("=="), is(true));
        assertThat(signatureString.endsWith("="), is(false));
    }

    private byte[] getDERPayloadBytes(byte[] payload)
            throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        KeyPair keyPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        Signature signature = Signature.getInstance("SHA256WithECDSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(payload);
        return signature.sign();
    }
}
