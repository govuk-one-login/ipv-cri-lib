package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class ConfigurationServiceTest {
    private static final String TEST_STACK_NAME = "stack-name";
    private static final String PARAM_NAME_FORMAT = "/%s/%s";
    private static final String PARAM_NAME = "param-name";
    private static final String PARAM_VALUE = "param-value";
    @Mock private SSMProvider mockSsmProvider;
    @Mock private SecretsProvider mockSecretsProvider;
    private ConfigurationService configurationService;

    @SystemStub
    private EnvironmentVariables environment =
            new EnvironmentVariables("AWS_REGION", "eu-west-2", "AWS_STACK_NAME", "my-stack-name");

    @Mock private ConfigurationService mockConfigurationService;

    private MockedStatic<Instant> mockedStatic;

    @AfterEach
    void afterEach() {
        mockedStatic.close();
    }

    @BeforeEach
    void setUp() {
        mockedStatic = Mockito.mockStatic(Instant.class);
        configurationService =
                new ConfigurationService(
                        mockSsmProvider,
                        mockSecretsProvider,
                        TEST_STACK_NAME,
                        null,
                        TEST_STACK_NAME);
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
                        mockSsmProvider, mockSecretsProvider, TEST_STACK_NAME, null, secretPrefix);
        String secretName = "my-secret-name";
        String secretValue = "secret-value";
        String fullSecretName = String.format(PARAM_NAME_FORMAT, secretPrefix, secretName);
        when(mockSecretsProvider.get(fullSecretName)).thenReturn(secretValue);
        assertEquals(secretValue, configurationService.getSecretValue(secretName));
        verify(mockSecretsProvider).get(fullSecretName);
    }

    @Test
    void shouldGetSessionExpirationEpoch() {
        long mockEpochSeconds = 1655203417;
        long sessionTtl = 10;
        when(mockSsmProvider.get(
                        String.format(
                                PARAM_NAME_FORMAT,
                                TEST_STACK_NAME,
                                ConfigurationService.SSMParameterName.SESSION_TTL.parameterName)))
                .thenReturn(String.valueOf(sessionTtl));

        Instant mockInstant = mock(Instant.class);
        Instant mockExpiration = mock(Instant.class);

        when(mockInstant.plus(anyLong(), eq(ChronoUnit.SECONDS))).thenReturn(mockExpiration);
        when(mockExpiration.getEpochSecond()).thenReturn(mockEpochSeconds + sessionTtl);
        mockedStatic.when(Instant::now).thenReturn(mockInstant);

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
                        TEST_STACK_NAME);

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
}
