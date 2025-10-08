package uk.gov.di.ipv.cri.common.library.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.lambda.powertools.parameters.secrets.SecretsProvider;
import software.amazon.lambda.powertools.parameters.ssm.SSMProvider;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class ClientProviderFactoryTest {
    @SystemStub private EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeEach
    void setUp() {
        environmentVariables.set("AWS_REGION", "eu-west-2");
        environmentVariables.set("AWS_STACK_NAME", "TEST_STACK");
        environmentVariables.set("AWS_CONTAINER_CREDENTIALS_FULL_URI", null);
    }

    @ParameterizedTest
    @CsvSource({"null", "HasValuePresent"})
    void shouldCreateClientProviderWithDefaultConstructorAndConfiguredCorrectly(
            String awsContainerCredentialsFullUri) {
        environmentVariables.set(
                "AWS_CONTAINER_CREDENTIALS_FULL_URI", awsContainerCredentialsFullUri);

        ClientProviderFactory clientProviderFactory = new ClientProviderFactory();

        AwsCredentialsProvider awsCredentialsProvider1 =
                clientProviderFactory.getAwsCredentialsProvider();
        assertNotNull(awsCredentialsProvider1);

        AwsCredentialsProvider awsCredentialsProvider2 =
                clientProviderFactory.getAwsCredentialsProvider();
        assertNotNull(awsCredentialsProvider2);

        assertEquals(awsCredentialsProvider1, awsCredentialsProvider2);

        if (null == awsContainerCredentialsFullUri) {
            assertEquals(
                    EnvironmentVariableCredentialsProvider.class,
                    awsCredentialsProvider1.getClass());
            assertEquals(
                    EnvironmentVariableCredentialsProvider.class,
                    awsCredentialsProvider2.getClass());
        } else {
            assertEquals(ContainerCredentialsProvider.class, awsCredentialsProvider1.getClass());
            assertEquals(ContainerCredentialsProvider.class, awsCredentialsProvider2.getClass());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "null, false, false", // Non-SnapStart, AutoTel
        "null, true, false", // Non-SnapStart, ManualTel, No Tracing Providers
        "null, true, true", // Non-SnapStart, ManualTel, Tracing Providers
        "HasValuePresent, false, false", // SnapStart, AutoTel
        "HasValuePresent, true, false", // SnapStart, ManualTel, No Tracing Providers
        "HasValuePresent, true, true", // SnapStart, ManualTel, Tracing Providers
    })
    void shouldCreateClientProviderWithParameterConstructorAndConfiguredCorrectly(
            String awsContainerCredentialsFullUri,
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {
        environmentVariables.set(
                "AWS_CONTAINER_CREDENTIALS_FULL_URI", awsContainerCredentialsFullUri);

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        AwsCredentialsProvider awsCredentialsProvider1 =
                clientProviderFactory.getAwsCredentialsProvider();
        assertNotNull(awsCredentialsProvider1);

        AwsCredentialsProvider awsCredentialsProvider2 =
                clientProviderFactory.getAwsCredentialsProvider();
        assertNotNull(awsCredentialsProvider2);

        assertEquals(awsCredentialsProvider1, awsCredentialsProvider2);

        if (null == awsContainerCredentialsFullUri) {
            assertEquals(
                    EnvironmentVariableCredentialsProvider.class,
                    awsCredentialsProvider1.getClass());
            assertEquals(
                    EnvironmentVariableCredentialsProvider.class,
                    awsCredentialsProvider2.getClass());
        } else {
            assertEquals(ContainerCredentialsProvider.class, awsCredentialsProvider1.getClass());
            assertEquals(ContainerCredentialsProvider.class, awsCredentialsProvider2.getClass());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "false, false", // AutoTel
        "true, false", // ManualTel, No Tracing Providers
        "true, true", // ManualTel, Tracing Providers
    })
    void shouldReturnKMSClient(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        KmsClient kmsClient1 = clientProviderFactory.getKMSClient();
        assertNotNull(kmsClient1);

        KmsClient kmsClient2 = clientProviderFactory.getKMSClient();
        assertNotNull(kmsClient2);

        assertEquals(kmsClient1, kmsClient2);
    }

    @ParameterizedTest
    @CsvSource({
        "false, false", // AutoTel
        "true, false", // ManualTel, No Tracing Providers
        "true, true", // ManualTel, Tracing Providers
    })
    void shouldReturnSqsClient(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        SqsClient sqsClient1 = clientProviderFactory.getSqsClient();
        assertNotNull(sqsClient1);

        SqsClient sqsClient2 = clientProviderFactory.getSqsClient();
        assertNotNull(sqsClient2);

        // Sqs Client is wrapped in a proxy when OpelTel is used breaking equals comparison
        // To confirm the wrapped sqsClient is the same we use the hashcode directly
        assertEquals(sqsClient1.hashCode(), sqsClient2.hashCode());
    }

    @ParameterizedTest
    @CsvSource({
        "false, false", // AutoTel
        "true, false", // ManualTel, No Tracing Providers
        "true, true", // ManualTel, Tracing Providers
    })
    void shouldReturnDynamoDbEnhancedClient(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        DynamoDbEnhancedClient dynamoDbEnhancedClient1 =
                clientProviderFactory.getDynamoDbEnhancedClient();
        assertNotNull(dynamoDbEnhancedClient1);

        DynamoDbEnhancedClient dynamoDbEnhancedClient2 =
                clientProviderFactory.getDynamoDbEnhancedClient();
        assertNotNull(dynamoDbEnhancedClient2);

        assertEquals(dynamoDbEnhancedClient1, dynamoDbEnhancedClient2);
    }

    @ParameterizedTest
    @CsvSource({
        "false, false", // AutoTel
        "true, false", // ManualTel, No Tracing Providers
        "true, true", // ManualTel, Tracing Providers
    })
    void shouldReturnSsmClient(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        SsmClient ssmClient1 = clientProviderFactory.getSsmClient();
        assertNotNull(ssmClient1);

        SsmClient ssmClient2 = clientProviderFactory.getSsmClient();
        assertNotNull(ssmClient2);

        assertEquals(ssmClient1, ssmClient2);
    }

    @ParameterizedTest
    @CsvSource({
        "false, false", // AutoTel
        "true, false", // ManualTel, No Tracing Providers
        "true, true", // ManualTel, Tracing Providers
    })
    void shouldReturnSSMProvider(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        SSMProvider ssmProvider1 = clientProviderFactory.getSSMProvider();
        assertNotNull(ssmProvider1);

        SSMProvider ssmProvider2 = clientProviderFactory.getSSMProvider();
        assertNotNull(ssmProvider2);

        assertEquals(ssmProvider1, ssmProvider2);
    }

    @ParameterizedTest
    @CsvSource({
        "false, false", // AutoTel
        "true, false", // ManualTel, No Tracing Providers
        "true, true", // ManualTel, Tracing Providers
    })
    void shouldReturnSecretsProvider(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        SecretsProvider secretsProvider1 = clientProviderFactory.getSecretsProvider();
        assertNotNull(secretsProvider1);

        SecretsProvider secretsProvider2 = clientProviderFactory.getSecretsProvider();
        assertNotNull(secretsProvider2);

        assertEquals(secretsProvider1, secretsProvider2);
    }

    @ParameterizedTest
    @CsvSource({
        "false, false", // AutoTel
        "true, false", // ManualTel, No Tracing Providers
        "true, true", // ManualTel, Tracing Providers
    })
    void shouldReturnSecretsManagerClient(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        SecretsManagerClient secretsManagerClient1 =
                clientProviderFactory.getSecretsManagerClient();
        assertNotNull(secretsManagerClient1);

        SecretsManagerClient secretsManagerClient2 =
                clientProviderFactory.getSecretsManagerClient();
        assertNotNull(secretsManagerClient2);

        assertEquals(secretsManagerClient1, secretsManagerClient2);
    }

    @ParameterizedTest
    @CsvSource({
        "false, false", // AutoTel
        "true, false", // ManualTel, No Tracing Providers
        "true, true", // ManualTel, Tracing Providers
    })
    void shouldReturnAcmClient(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {

        ClientProviderFactory clientProviderFactory =
                new ClientProviderFactory(
                        usingNonAutomaticOpenTelemetry,
                        avoidExecutionInterceptorsOnClientsUsedByPowerTools);

        AcmClient acmClient1 = clientProviderFactory.getAcmClient();
        assertNotNull(acmClient1);

        AcmClient acmClient2 = clientProviderFactory.getAcmClient();
        assertNotNull(acmClient2);

        assertEquals(acmClient1, acmClient2);
    }
}
