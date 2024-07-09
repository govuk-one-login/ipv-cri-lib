package uk.gov.di.ipv.cri.common.library.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class ClientProviderFactoryTest {
    @SystemStub private EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private ClientProviderFactory clientProviderFactory;

    @BeforeEach
    void setUp() {
        environmentVariables.set("AWS_REGION", "eu-west-2");
        environmentVariables.set("AWS_STACK_NAME", "TEST_STACK");

        clientProviderFactory = new ClientProviderFactory();
    }

    @Test
    void shouldReturnKMSClient() {

        KmsClient kmsClient = clientProviderFactory.getKMSClient();

        assertNotNull(kmsClient);
    }

    @Test
    void shouldReturnSqsClient() {

        SqsClient sqsClient = clientProviderFactory.getSqsClient();

        assertNotNull(sqsClient);
    }

    @Test
    void shouldReturnDynamoDbEnhancedClient() {

        DynamoDbEnhancedClient dynamoDbEnhancedClient =
                clientProviderFactory.getDynamoDbEnhancedClient();

        assertNotNull(dynamoDbEnhancedClient);
    }

    @Test
    void shouldReturnSSMProvider() {

        SSMProvider ssmProvider = clientProviderFactory.getSSMProvider();

        assertNotNull(ssmProvider);
    }

    @Test
    void shouldReturnSecretsProvider() {

        SecretsProvider secretsProvider = clientProviderFactory.getSecretsProvider();

        assertNotNull(secretsProvider);
    }
}
