package uk.gov.di.ipv.cri.common.library.config;

public class ApiGatewayConfig {
    public String getPrivateApiEndpoint() {
        return getApiEndpoint("API_GATEWAY_ID_PRIVATE");
    }

    public String getPublicApiEndpoint() {
        return getApiEndpoint("API_GATEWAY_ID_PUBLIC");
    }

    public String getPublicApiKey() {
        return getApiEndpoint("APIGW_API_KEY");
    }

    private String getApiEndpoint(String key) {
        String id = EnvironConfig.getEnv(key);

        return String.format("https://%s.execute-api.eu-west-2.amazonaws.com", id);
    }
}
