package uk.gov.di.ipv.cri.common.library.config;

public final class ApiGateway {

    public static String getPrivateApiEndpoint() {
        return getApiEndpoint("API_GATEWAY_ID_PRIVATE");
    }

    public static String getPublicApiEndpoint() {
        return getApiEndpoint("API_GATEWAY_ID_PUBLIC");
    }

    public static String getPublicApiKey() {
        return Environment.getEnv("APIGW_API_KEY"); // Not an endpoint!
    }

    private static String getApiEndpoint(String key) {
        String id = Environment.getEnv(key);
        return String.format("https://%s.execute-api.eu-west-2.amazonaws.com", id);
    }
}
