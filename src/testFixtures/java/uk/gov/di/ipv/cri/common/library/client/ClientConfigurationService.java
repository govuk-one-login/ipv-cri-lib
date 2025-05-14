package uk.gov.di.ipv.cri.common.library.client;

import java.util.Objects;
import java.util.Optional;

public class ClientConfigurationService {
    private static final String MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT =
            "Environment variable %s is not set";
    private final String environment;
    private final String privateApiEndpoint;
    private final String publicApiEndpoint;
    private final String publicApiKey;
    private final String testResourcesStackName;
    private final String clientId;
    private final String commonStackName;

    public ClientConfigurationService() {
        this.environment =
                Objects.requireNonNull(
                        System.getenv("ENVIRONMENT"),
                        String.format(MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT, "ENVIRONMENT"));
        this.privateApiEndpoint =
                getApiEndpoint(
                        "API_GATEWAY_ID_PRIVATE",
                        String.format(
                                MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT, "API_GATEWAY_ID_PRIVATE"));
        this.publicApiEndpoint =
                getApiEndpoint(
                        "API_GATEWAY_ID_PUBLIC",
                        String.format(
                                MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT, "API_GATEWAY_ID_PUBLIC"));
        this.publicApiKey =
                Objects.requireNonNull(
                        System.getenv("APIGW_API_KEY"),
                        String.format(MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT, "APIGW_API_KEY"));
        this.testResourcesStackName =
                Optional.ofNullable(System.getenv("TEST_RESOURCES_STACK_NAME"))
                        .orElse("test-resources");
        this.clientId =
                Optional.ofNullable(System.getenv("DEFAULT_CLIENT_ID"))
                        .orElse("ipv-core-stub-aws-headless");
        this.commonStackName =
                Optional.ofNullable(System.getenv("COMMON_STACK_NAME")).orElse("common-cri-api");
    }

    public String getPrivateApiEndpoint() {
        return this.privateApiEndpoint;
    }

    public String getPublicApiEndpoint() {
        return this.publicApiEndpoint;
    }

    public String getPublicApiKey() {
        return this.publicApiKey;
    }

    public String getTestResourcesStackName() {
        return this.testResourcesStackName;
    }

    public String createUriPath(String endpoint) {
        return String.format("/%s/%s", this.environment, endpoint);
    }

    public String getDefaultClientId() {
        return this.clientId;
    }

    public String getCommonStackName() {
        return this.commonStackName;
    }

    private static String getApiEndpoint(String apikey, String message) {
        String restApiId =
                Optional.ofNullable(System.getenv(apikey))
                        .orElseThrow(() -> new IllegalArgumentException(message));

        return String.format("https://%s.execute-api.eu-west-2.amazonaws.com", restApiId);
    }
}
