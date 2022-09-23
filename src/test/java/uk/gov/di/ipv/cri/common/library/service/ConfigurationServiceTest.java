package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.common.library.service.ConfigurationService.CONFIG_SERVICE_CACHE_TTL_MINS;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class ConfigurationServiceTest {
    private static final String TEST_STACK_NAME = "stack-name";
    private static final String PARAM_NAME_FORMAT = "/%s/%s";
    private static final String PARAM_NAME = "param-name";
    private static final String PARAM_VALUE = "param-value";
    @Mock private SSMProvider mockSsmProvider;
    @Mock private SecretsProvider mockSecretsProvider;
    @Mock private Clock mockClock;
    private ConfigurationService configurationService;

    @SystemStub
    private EnvironmentVariables environment =
            new EnvironmentVariables("AWS_REGION", "eu-west-2", "AWS_STACK_NAME", "my-stack-name");

    @BeforeEach
    void setUp() {
        configurationService =
                new ConfigurationService(
                        mockSsmProvider,
                        mockSecretsProvider,
                        TEST_STACK_NAME,
                        null,
                        TEST_STACK_NAME,
                        mockClock);
    }

    @Test
    void shouldGetParamValueByName() {
        String fullParamName = String.format(PARAM_NAME_FORMAT, TEST_STACK_NAME, PARAM_NAME);
        when(mockSsmProvider.get(fullParamName)).thenReturn(PARAM_VALUE);
        assertEquals(PARAM_VALUE, configurationService.getParameterValue(PARAM_NAME));
        verify(mockSsmProvider).get(fullParamName);
    }

    @Test
    void shouldGetSecretValueByNameThatHasTheStackName() {
        String secretName = "my-secret-name";
        String secretValue = "secret-value";
        String fullSecretName = String.format(PARAM_NAME_FORMAT, TEST_STACK_NAME, secretName);
        when(mockSecretsProvider.get(fullSecretName)).thenReturn(secretValue);
        assertEquals(secretValue, configurationService.getSecretValue(secretName));
        verify(mockSecretsProvider).get(fullSecretName);
    }

    @Test
    void shouldGetSecretValueByNameThatTheSecretPrefixProvided() {
        String secretPrefix = "customSecretPrefix";
        configurationService =
                new ConfigurationService(
                        mockSsmProvider,
                        mockSecretsProvider,
                        TEST_STACK_NAME,
                        null,
                        secretPrefix,
                        mockClock);
        String secretName = "my-secret-name";
        String secretValue = "secret-value";
        String fullSecretName = String.format(PARAM_NAME_FORMAT, secretPrefix, secretName);
        when(mockSecretsProvider.get(fullSecretName)).thenReturn(secretValue);
        assertEquals(secretValue, configurationService.getSecretValue(secretName));
        verify(mockSecretsProvider).get(fullSecretName);
    }

    @Test
    void shouldGetSessionExpirationEpoch() {
        long sessionTtl = 10;
        when(mockSsmProvider.get(
                        String.format(
                                PARAM_NAME_FORMAT,
                                TEST_STACK_NAME,
                                ConfigurationService.SSMParameterName.SESSION_TTL.parameterName)))
                .thenReturn(String.valueOf(sessionTtl));
        when(mockClock.instant()).thenReturn(Instant.ofEpochSecond(1655203417));
        assertEquals(1655203427, configurationService.getSessionExpirationEpoch());
    }

    @Test
    void shouldGetSessionTtl() {
        long sessionTtl = 10;
        when(mockSsmProvider.get(
                        String.format(
                                PARAM_NAME_FORMAT,
                                TEST_STACK_NAME,
                                ConfigurationService.SSMParameterName.SESSION_TTL.parameterName)))
                .thenReturn(String.valueOf(sessionTtl));
        assertEquals(sessionTtl, configurationService.getSessionTtl());
    }

    @Test
    void shouldGetCommonParameterValueUsingCommonPrefix() {
        String commonParamPrefix = "common-param-prefix";
        configurationService =
                new ConfigurationService(
                        mockSsmProvider,
                        mockSecretsProvider,
                        TEST_STACK_NAME,
                        commonParamPrefix,
                        TEST_STACK_NAME,
                        mockClock);

        when(mockSsmProvider.get(String.format(PARAM_NAME_FORMAT, commonParamPrefix, PARAM_NAME)))
                .thenReturn(PARAM_VALUE);
        assertEquals(PARAM_VALUE, configurationService.getCommonParameterValue(PARAM_NAME));
        verify(mockSsmProvider)
                .get(String.format(PARAM_NAME_FORMAT, commonParamPrefix, PARAM_NAME));
    }

    @Test
    void shouldGetParameterByAbsoluteName() {
        when(mockSsmProvider.get(PARAM_NAME)).thenReturn(PARAM_VALUE);
        assertEquals(PARAM_VALUE, configurationService.getParameterValueByAbsoluteName(PARAM_NAME));
        verify(mockSsmProvider).get(PARAM_NAME);
    }

    @Test
    @DisplayName("should cache ssm params and secrets manager secrets for 5 minutes by default")
    void cacheFor5MinsByDefault() {
        testConfigurationManagerWithExpectedCacheMinutes(5);
    }

    @Test
    @DisplayName(
            "should cache ssm params and secrets manager secrets for 1 minute if set by the env var " + CONFIG_SERVICE_CACHE_TTL_MINS)
    void cacheForMinutesSetByEnvVar() {
        environment.set(CONFIG_SERVICE_CACHE_TTL_MINS, "1");
        testConfigurationManagerWithExpectedCacheMinutes(1);
    }

    private void testConfigurationManagerWithExpectedCacheMinutes(int expectedMinutes) {
        try (MockedStatic<ParamManager> mockStaticParamManager = mockStatic(ParamManager.class);
                MockedStatic<SsmClient> mockStaticSsmClient = mockStatic(SsmClient.class);
                MockedStatic<SecretsManagerClient> mockStaticSecretsManagerClient =
                        mockStatic(SecretsManagerClient.class)) {

            {
                SsmClientBuilder mockSsmClientBuilder = mock(SsmClientBuilder.class);
                mockStaticSsmClient.when(SsmClient::builder).thenReturn(mockSsmClientBuilder);
                SsmClient mockSsmClient = mock(SsmClient.class);
                when(mockSsmClientBuilder.region(Region.EU_WEST_2))
                        .thenReturn(mockSsmClientBuilder);
                when(mockSsmClientBuilder.httpClient(any(UrlConnectionHttpClient.class)))
                        .thenReturn(mockSsmClientBuilder);
                when(mockSsmClientBuilder.build()).thenReturn(mockSsmClient);
                mockStaticParamManager
                        .when(() -> ParamManager.getSsmProvider(mockSsmClient))
                        .thenReturn(mockSsmProvider);
            }

            {
                SecretsManagerClientBuilder mockSecretsManagerClientBuilder =
                        mock(SecretsManagerClientBuilder.class);
                SecretsManagerClient mockSecretsManagerClient = mock(SecretsManagerClient.class);
                mockStaticSecretsManagerClient
                        .when(SecretsManagerClient::builder)
                        .thenReturn(mockSecretsManagerClientBuilder);
                when(mockSecretsManagerClientBuilder.region(Region.EU_WEST_2))
                        .thenReturn(mockSecretsManagerClientBuilder);
                when(mockSecretsManagerClientBuilder.httpClient(any(UrlConnectionHttpClient.class)))
                        .thenReturn(mockSecretsManagerClientBuilder);
                when(mockSecretsManagerClientBuilder.build()).thenReturn(mockSecretsManagerClient);
                mockStaticParamManager
                        .when(() -> ParamManager.getSecretsProvider(mockSecretsManagerClient))
                        .thenReturn(mockSecretsProvider);
            }

            new ConfigurationService();

            verify(mockSsmProvider).defaultMaxAge(expectedMinutes, ChronoUnit.MINUTES);
            verify(mockSecretsProvider).defaultMaxAge(expectedMinutes, ChronoUnit.MINUTES);
        }
    }
}
