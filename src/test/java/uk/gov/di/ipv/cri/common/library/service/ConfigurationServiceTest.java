package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.common.library.service.ConfigurationService.SSMParameterName.AUTH_REQUEST_KMS_ENCRYPTION_KEY_ID;
import static uk.gov.di.ipv.cri.common.library.service.ConfigurationService.SSMParameterName.SESSION_TTL;
import static uk.gov.di.ipv.cri.common.library.service.ConfigurationService.SSMParameterName.VERIFIABLE_CREDENTIAL_SIGNING_KEY_ID;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class ConfigurationServiceTest {
    private static final String PARAM_NAME_FORMAT = "/%s/%s";
    private static final String PARAM_NAME = "param-name";
    private static final String PARAM_VALUE = "param-value";

    @Mock private SSMProvider mockSsmProvider;
    @Mock private SecretsProvider mockSecretsProvider;
    @Mock private Clock mockClock;
    private ConfigurationService configurationService;

    @SystemStub
    private EnvironmentVariables environment =
            new EnvironmentVariables(
                    "AWS_REGION",
                    "eu-west-2",
                    "AWS_STACK_NAME",
                    "my-stack-name",
                    "SQS_AUDIT_EVENT_QUEUE_URL",
                    "https://test-audit-queue",
                    "SQS_AUDIT_EVENT_PREFIX",
                    "AUDIT-PREFIX",
                    "VERIFIABLE_CREDENTIAL_ISSUER",
                    "http://test-vc/issuer");

    private String stackName = environment.getVariables().get("AWS_STACK_NAME");

    @BeforeEach
    void setUp() {
        configurationService =
                new ConfigurationService(
                        mockSsmProvider,
                        mockSecretsProvider,
                        stackName,
                        null,
                        stackName,
                        mockClock);

        String fullParamName = String.format(PARAM_NAME_FORMAT, stackName, PARAM_NAME);
        when(mockSsmProvider.get(fullParamName)).thenReturn(PARAM_VALUE);

        assertEquals(PARAM_VALUE, configurationService.getParameterValue(PARAM_NAME));
        verify(mockSsmProvider).get(fullParamName);
    }

    @Nested
    class ConfigServiceGetSecrets {
        private static final String SECRET_NAME = "my-secret-name";
        private static final String SECRET_VALUE = "secret-value";

        @Test
        void shouldGetSecretValueByNameThatHasTheStackName() {
            String fullSecretName = String.format(PARAM_NAME_FORMAT, stackName, SECRET_NAME);

            when(mockSecretsProvider.get(fullSecretName)).thenReturn(SECRET_VALUE);

            assertEquals(SECRET_VALUE, configurationService.getSecretValue(SECRET_NAME));
            verify(mockSecretsProvider).get(fullSecretName);
        }

        @Test
        void shouldGetSecretValueByNameThatTheSecretPrefixProvided() {
            String secretPrefix = "customSecretPrefix";
            configurationService =
                    new ConfigurationService(
                            mockSsmProvider,
                            mockSecretsProvider,
                            stackName,
                            null,
                            secretPrefix,
                            mockClock);

            String fullSecretName = String.format(PARAM_NAME_FORMAT, secretPrefix, SECRET_NAME);
            when(mockSecretsProvider.get(fullSecretName)).thenReturn(SECRET_VALUE);

            assertEquals(SECRET_VALUE, configurationService.getSecretValue(SECRET_NAME));
            verify(mockSecretsProvider).get(fullSecretName);
        }
    }

    @Nested
    class ConfigServiceGetParameters {
        @Test
        void shouldGetVerifiableCredentialKmsSigningKeyId() {
            String vcSigningKmsId = UUID.randomUUID().toString();
            String fullVcKmsSigningKeyPath =
                    String.format(
                            PARAM_NAME_FORMAT,
                            stackName,
                            VERIFIABLE_CREDENTIAL_SIGNING_KEY_ID.parameterName);
            when(mockSsmProvider.get(fullVcKmsSigningKeyPath)).thenReturn(vcSigningKmsId);

            assertEquals(
                    vcSigningKmsId, configurationService.getVerifiableCredentialKmsSigningKeyId());

            verify(mockSsmProvider).get(fullVcKmsSigningKeyPath);
        }

        @Test
        void shouldGetKmsEncryptionKeyId() {
            String vcKmsEncryptionKeyId = UUID.randomUUID().toString();
            String fullKmsEncryptionKeyPath =
                    String.format(
                            PARAM_NAME_FORMAT,
                            stackName,
                            AUTH_REQUEST_KMS_ENCRYPTION_KEY_ID.parameterName);
            when(mockSsmProvider.get(fullKmsEncryptionKeyPath)).thenReturn(vcKmsEncryptionKeyId);

            assertEquals(vcKmsEncryptionKeyId, configurationService.getKmsEncryptionKeyId());

            verify(mockSsmProvider).get(fullKmsEncryptionKeyPath);
        }

        @Test
        void shouldGetParameterByAbsoluteName() {
            when(mockSsmProvider.get(PARAM_NAME)).thenReturn(PARAM_VALUE);

            assertEquals(
                    PARAM_VALUE, configurationService.getParameterValueByAbsoluteName(PARAM_NAME));
            verify(mockSsmProvider).get(PARAM_NAME);
        }
    }

    @Nested
    class ConfigServiceGetCommonParameters {
        private static final String COMMON_PARAM_PREFIX = "common-param-prefix";
        private String fullPathSessionTtl =
                String.format(PARAM_NAME_FORMAT, COMMON_PARAM_PREFIX, SESSION_TTL.parameterName);

        @BeforeEach
        void setUp() {
            configurationService =
                    new ConfigurationService(
                            mockSsmProvider,
                            mockSecretsProvider,
                            stackName,
                            COMMON_PARAM_PREFIX,
                            stackName,
                            mockClock);

            String fullPathParams =
                    String.format(PARAM_NAME_FORMAT, COMMON_PARAM_PREFIX, PARAM_NAME);
            when(mockSsmProvider.get(fullPathParams)).thenReturn(PARAM_VALUE);

            assertEquals(PARAM_VALUE, configurationService.getCommonParameterValue(PARAM_NAME));
            verify(mockSsmProvider).get(fullPathParams);
        }

        @Test
        void shouldGetSessionExpirationEpoch() {
            long sessionTtl = 10;

            when(mockSsmProvider.get(fullPathSessionTtl)).thenReturn(String.valueOf(sessionTtl));

            when(mockClock.instant()).thenReturn(Instant.ofEpochSecond(1655203417));

            assertEquals(1655203427, configurationService.getSessionExpirationEpoch());
            verify(mockSsmProvider).get(fullPathSessionTtl);
        }

        @Test
        void shouldGetSessionTtl() {
            long sessionTtl = 10;

            when(mockSsmProvider.get(fullPathSessionTtl)).thenReturn(String.valueOf(sessionTtl));

            assertEquals(sessionTtl, configurationService.getSessionTtl());
            verify(mockSsmProvider).get(fullPathSessionTtl);
        }
    }

    @Nested
    class ConfigServiceGetEnvironmentVariables {
        @Test
        void shouldGetSqsAuditEventQueueUrlFromEnv() {
            String audiEventQueueUrl = configurationService.getSqsAuditEventQueueUrl();

            assertEquals("https://test-audit-queue", audiEventQueueUrl);
        }

        @Test
        void shouldGetAuditEventPrefixFromEnv() {
            String audiEventQueueUrl = configurationService.getSqsAuditEventPrefix();

            assertEquals("AUDIT-PREFIX", audiEventQueueUrl);
        }

        @Test
        void shouldGetDefaultMaxJwtTtl() {
            environment.set("MAXIMUM_JWT_TTL", null);

            assertEquals(6, configurationService.getMaxJwtTtl());
        }

        @Test
        void shouldGetExplicitlySetMaxJwtTtl() {
            environment.set("MAXIMUM_JWT_TTL", "10");

            assertEquals(10, configurationService.getMaxJwtTtl());
        }

        @Test
        void shouldGetVerifiableCredentialIssuer() {
            String vcIssuer = "http://test-vc/issuer";

            assertEquals(vcIssuer, configurationService.getVerifiableCredentialIssuer());
        }

        @Test
        void shouldThrowExceptionWhenSqsAuditEventQueueUrlIsMissing() {
            environment.set("SQS_AUDIT_EVENT_QUEUE_URL", null);

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> configurationService.getSqsAuditEventQueueUrl());

            assertEquals(
                    "Environment variable SQS_AUDIT_EVENT_QUEUE_URL is not set",
                    exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenSqsAuditEventPrefixIsMissing() {
            environment.set("SQS_AUDIT_EVENT_PREFIX", null);

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> configurationService.getSqsAuditEventPrefix());

            assertEquals(
                    "Environment variable SQS_AUDIT_EVENT_PREFIX is not set",
                    exception.getMessage());
        }

        @Test
        void shouldThrowExceptionWhenVerifiableCredentialIssuerIsMissing() {
            environment.set("VERIFIABLE_CREDENTIAL_ISSUER", null);

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            configurationService::getVerifiableCredentialIssuer);

            assertEquals(
                    "Environment variable VERIFIABLE_CREDENTIAL_ISSUER is not set",
                    exception.getMessage());
        }
    }
}
