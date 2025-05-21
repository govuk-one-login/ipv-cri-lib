package uk.gov.di.ipv.cri.common.library.client;

import uk.gov.di.ipv.cri.common.library.config.ApiGateway;
import uk.gov.di.ipv.cri.common.library.config.Environment;

public class ClientConfigurationService {
    private final String environment;
    private final String privateApiEndpoint;
    private final String publicApiEndpoint;
    private final String publicApiKey;
    private final String testResourcesStackName;
    private final String clientId;
    private final String commonStackName;

    public ClientConfigurationService() {

        this.privateApiEndpoint = ApiGateway.getPrivateApiEndpoint();
        this.publicApiEndpoint = ApiGateway.getPublicApiEndpoint();
        this.publicApiKey = ApiGateway.getPublicApiKey();

        this.environment = Environment.getEnv("ENVIRONMENT");
        this.testResourcesStackName =
                Environment.getEnvOrDefault("TEST_RESOURCES_STACK_NAME", "test-resources");
        this.clientId =
                Environment.getEnvOrDefault("DEFAULT_CLIENT_ID", "ipv-core-stub-aws-headless");
        this.commonStackName = Environment.getEnvOrDefault("COMMON_STACK_NAME", "common-cri-api");
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
}
