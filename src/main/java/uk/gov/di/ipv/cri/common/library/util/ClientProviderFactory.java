package uk.gov.di.ipv.cri.common.library.util;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.acm.AcmClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.lambda.powertools.parameters.secrets.SecretsProvider;
import software.amazon.lambda.powertools.parameters.ssm.SSMProvider;

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
    private final AwsCredentialsProvider awsCredentialsProvider;

    // https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/awscore/defaultsmode/DefaultsMode.html
    // https://docs.aws.amazon.com/sdkref/latest/guide/feature-smart-config-defaults.html
    // Optmize for within the same region
    private static final DefaultsMode DEFAULTS_MODE = DefaultsMode.IN_REGION;

    // All clients are Lazy init to prevent creating them when not used
    private KmsClient kmsClient;
    private SqsClient sqsClient;
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private SsmClient ssmClient;
    private SSMProvider ssmProvider;
    private SecretsProvider secretsProvider;
    private SecretsManagerClient secretsManagerClient;
    private AcmClient acmClient;

    private final boolean addOpenTelemetryExecutionInterceptors;
    // Override to work around a clash at Run-time with Dynatrace Agent
    private final boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools;

    public ClientProviderFactory() {
        // The CRI is using opentelemetry-aws-sdk-2.2-autoconfigure
        this(false, false);
    }

    public ClientProviderFactory(
            boolean usingNonAutomaticOpenTelemetry,
            boolean avoidExecutionInterceptorsOnClientsUsedByPowerTools) {
        this.addOpenTelemetryExecutionInterceptors = usingNonAutomaticOpenTelemetry;
        this.avoidExecutionInterceptorsOnClientsUsedByPowerTools =
                avoidExecutionInterceptorsOnClientsUsedByPowerTools;

        awsRegion = Region.of(System.getenv("AWS_REGION"));

        // AWS SDK CRT Client (SYNC) - connection defaults are in SdkHttpConfigurationOption
        sdkHttpClient = AwsCrtHttpClient.builder().maxConcurrency(100).build();

        // Check if started inside a snap start container and use appropriate provider
        // see https://docs.aws.amazon.com/lambda/latest/dg/snapstart-activate.html
        awsCredentialsProvider =
                System.getenv("AWS_CONTAINER_CREDENTIALS_FULL_URI") == null
                        ? EnvironmentVariableCredentialsProvider.create()
                        : ContainerCredentialsProvider.builder().build();
    }

    public AwsCredentialsProvider getAwsCredentialsProvider() {
        return awsCredentialsProvider;
    }

    public KmsClient getKMSClient() {

        if (null == kmsClient) {
            KmsClientBuilder kmsClientBuilder =
                    KmsClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(awsCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE);

            if (addOpenTelemetryExecutionInterceptors) {
                kmsClientBuilder.overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(
                                        AwsSdkTelemetry.create(GlobalOpenTelemetry.get())
                                                .newExecutionInterceptor())
                                .build());
            }

            kmsClient = kmsClientBuilder.build();
        }

        return kmsClient;
    }

    public SqsClient getSqsClient() {

        if (null == sqsClient) {
            SqsClientBuilder sqsClientBuilder =
                    SqsClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(awsCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE);

            if (addOpenTelemetryExecutionInterceptors) {
                OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
                AwsSdkTelemetry awsSdkTelemetry = AwsSdkTelemetry.create(openTelemetry);

                sqsClientBuilder.overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(awsSdkTelemetry.newExecutionInterceptor())
                                .build());

                sqsClient = awsSdkTelemetry.wrap(sqsClientBuilder.build());
            } else {
                sqsClient = sqsClientBuilder.build();
            }
        }

        return sqsClient;
    }

    public DynamoDbEnhancedClient getDynamoDbEnhancedClient() {
        if (null == dynamoDbEnhancedClient) {
            DynamoDbClientBuilder dynamoDbClientBuilder =
                    DynamoDbClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(awsCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE);

            if (addOpenTelemetryExecutionInterceptors) {
                dynamoDbClientBuilder.overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(
                                        AwsSdkTelemetry.create(GlobalOpenTelemetry.get())
                                                .newExecutionInterceptor())
                                .build());
            }

            DynamoDbClient dynamoDbClient = dynamoDbClientBuilder.build();

            dynamoDbEnhancedClient =
                    DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        }

        return dynamoDbEnhancedClient;
    }

    public SsmClient getSsmClient() {

        if (null == ssmClient) {
            SsmClientBuilder ssmClientBuilder =
                    SsmClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(awsCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE);

            if (addOpenTelemetryExecutionInterceptors
                    && !avoidExecutionInterceptorsOnClientsUsedByPowerTools) {
                ssmClientBuilder.overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(
                                        AwsSdkTelemetry.create(GlobalOpenTelemetry.get())
                                                .newExecutionInterceptor())
                                .build());
            }

            ssmClient = ssmClientBuilder.build();
        }

        return ssmClient;
    }

    // ThreadLocalRandom not used cryptographically here
    @java.lang.SuppressWarnings("java:S2245")
    public SSMProvider getSSMProvider() {
        if (null == ssmProvider) {

            int maxAge = generateRandomMaxAgeInSeconds();
            LOGGER.info("PowerTools SSMProvider defaultMaxAge selected as {} seconds", maxAge);

            ssmProvider = SSMProvider.builder().withClient(getSsmClient()).build();
            ssmProvider.withMaxAge(maxAge, ChronoUnit.SECONDS);
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
                    SecretsProvider.builder().withClient(getSecretsManagerClient()).build();
            secretsProvider.withMaxAge(maxAge, ChronoUnit.SECONDS);
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

    public SecretsManagerClient getSecretsManagerClient() {
        if (null == secretsManagerClient) {

            SecretsManagerClientBuilder secretsManagerClientBuilder =
                    SecretsManagerClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(awsCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE);

            if (addOpenTelemetryExecutionInterceptors) {
                secretsManagerClientBuilder.overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(
                                        AwsSdkTelemetry.create(GlobalOpenTelemetry.get())
                                                .newExecutionInterceptor())
                                .build());
            }

            secretsManagerClient = secretsManagerClientBuilder.build();
        }

        return secretsManagerClient;
    }

    public AcmClient getAcmClient() {
        if (null == acmClient) {

            AcmClientBuilder acmClientBuilder =
                    AcmClient.builder()
                            .region(awsRegion)
                            .httpClient(sdkHttpClient)
                            .credentialsProvider(awsCredentialsProvider)
                            .defaultsMode(DEFAULTS_MODE);

            if (addOpenTelemetryExecutionInterceptors) {
                acmClientBuilder.overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(
                                        AwsSdkTelemetry.create(GlobalOpenTelemetry.get())
                                                .newExecutionInterceptor())
                                .build());
            }

            acmClient = acmClientBuilder.build();
        }

        return acmClient;
    }
}
