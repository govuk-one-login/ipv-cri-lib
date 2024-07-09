package uk.gov.di.ipv.cri.common.library.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.lambda.powertools.parameters.ParamManager;
import software.amazon.lambda.powertools.parameters.SSMProvider;
import software.amazon.lambda.powertools.parameters.SecretsProvider;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public class ClientProviderFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MIN_MINUTES =
            "CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MIN_MINUTES";
    public static final String CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MAX_MINUTES =
            "CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MAX_MINUTES";

    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/best-practices.html#bestpractice1
    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/http-configuration.html
    private final SdkHttpClient sdkHttpClient;
    // https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/lambda-optimize-starttime.html
    // https://aws.amazon.com/blogs/developer/tuning-the-aws-java-sdk-2-x-to-reduce-startup-time/
    private final Region awsRegion;
    private final EnvironmentVariableCredentialsProvider environmentVariableCredentialsProvider;

    // https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/awscore/defaultsmode/DefaultsMode.html
    // https://docs.aws.amazon.com/sdkref/latest/guide/feature-smart-config-defaults.html
    // Optmize for within the same region
    private static final DefaultsMode DEFAULTS_MODE = DefaultsMode.IN_REGION;

    // All clients are Lazy init to prevent creating them when not used
    private KmsClient kmsClient;
    private SqsClient sqsClient;
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private SSMProvider ssmProvider;
    private SecretsProvider secretsProvider;
    private SecretsManagerClient secretsManagerClient;

    public ClientProviderFactory() {
        awsRegion = Region.of(System.getenv("AWS_REGION"));
        // AWS SDK CRT Client (SYNC) - connection defaults are in SdkHttpConfigurationOption
        sdkHttpClient = AwsCrtHttpClient.builder().maxConcurrency(100).build();
        environmentVariableCredentialsProvider = EnvironmentVariableCredentialsProvider.create();
    }

    public KmsClient getKMSClient() {

        if (null == kmsClient) {
            kmsClient =
                    KmsClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(environmentVariableCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE)
                            .build();
        }

        return kmsClient;
    }

    public SqsClient getSqsClient() {

        if (null == sqsClient) {
            sqsClient =
                    SqsClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(environmentVariableCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE)
                            .build();
        }

        return sqsClient;
    }

    public DynamoDbEnhancedClient getDynamoDbEnhancedClient() {

        if (null == dynamoDbEnhancedClient) {
            DynamoDbClient dynamoDbClient =
                    DynamoDbClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(environmentVariableCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE)
                            .build();

            dynamoDbEnhancedClient =
                    DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        }

        return dynamoDbEnhancedClient;
    }

    // ThreadLocalRandom not used cryptographically here
    @java.lang.SuppressWarnings("java:S2245")
    public SSMProvider getSSMProvider() {

        if (null == ssmProvider) {
            SsmClient ssmClient =
                    SsmClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(environmentVariableCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE)
                            .build();

            int maxAge = generateRandomMaxAgeInSeconds();

            LOGGER.info("PowerTools SSMProvider defaultMaxAge selected as {} seconds", maxAge);

            ssmProvider =
                    ParamManager.getSsmProvider(ssmClient)
                            .defaultMaxAge(maxAge, ChronoUnit.SECONDS);
        }

        return ssmProvider;
    }

    // ThreadLocalRandom not used cryptographically here
    @java.lang.SuppressWarnings("java:S2245")
    public SecretsProvider getSecretsProvider() {

        if (null == secretsProvider) {

            int maxAge = generateRandomMaxAgeInSeconds();

            LOGGER.info("PowerTools SecretsProvider defaultMaxAge selected as {} seconds", maxAge);

            secretsProvider =
                    ParamManager.getSecretsProvider(getSecretsManagerClient())
                            .defaultMaxAge(maxAge, ChronoUnit.SECONDS);
        }
        return secretsProvider;
    }

    // ThreadLocalRandom not used cryptographically here
    @java.lang.SuppressWarnings("java:S2245")
    private int generateRandomMaxAgeInSeconds() {
        // If no values are set these are the fallback values
        int minCacheAgeFallback = 5;
        int maxCacheAgeFallback = 15;

        int cacheMinMinutes =
                System.getenv(CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MIN_MINUTES) != null
                        ? Integer.parseInt(
                                System.getenv(CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MIN_MINUTES))
                        : minCacheAgeFallback;

        int cacheMaxMinutes =
                System.getenv(CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MAX_MINUTES) != null
                        ? Integer.parseInt(
                                System.getenv(CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MAX_MINUTES))
                        : maxCacheAgeFallback;

        int minCacheSeconds = cacheMinMinutes * 60;
        int maxCacheSeconds = cacheMaxMinutes * 60;

        return ThreadLocalRandom.current().nextInt(maxCacheSeconds - minCacheSeconds + 1)
                + minCacheSeconds;
    }

    private SecretsManagerClient getSecretsManagerClient() {
        if (null == secretsManagerClient) {
            secretsManagerClient =
                    SecretsManagerClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(environmentVariableCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE)
                            .build();
        }

        return secretsManagerClient;
    }
}
