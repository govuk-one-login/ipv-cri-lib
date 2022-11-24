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
    private final String ipvCoreStubUrl;
    private final String ipvCoreStubUsername;
    private final String ipvCoreStubPassword;
    private final String ipvCoreStubCriId;

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
        this.ipvCoreStubUrl =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_URL"),
                        String.format(MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT, "IPV_CORE_STUB_URL"));
        this.ipvCoreStubUsername =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_USER"),
                        String.format(
                                MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT,
                                "IPV_CORE_STUB_BASIC_AUTH_USER"));
        this.ipvCoreStubPassword =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_BASIC_AUTH_PASSWORD"),
                        String.format(
                                MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT,
                                "IPV_CORE_STUB_BASIC_AUTH_PASSWORD"));
        this.ipvCoreStubCriId =
                Objects.requireNonNull(
                        System.getenv("IPV_CORE_STUB_CRI_ID"),
                        String.format(
                                MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT, "IPV_CORE_STUB_CRI_ID"));
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

    public String getIPVCoreStubURL() {
        return this.ipvCoreStubUrl;
    }

    public String getIpvCoreStubUsername() {
        return this.ipvCoreStubUsername;
    }

    public String getIpvCoreStubPassword() {
        return this.ipvCoreStubPassword;
    }

    public String getIpvCoreStubCriId() {
        return ipvCoreStubCriId;
    }

    public String createUriPath(String endpoint) {
        return String.format("/%s/%s", this.environment, endpoint);
    }

    private static String getApiEndpoint(String apikey, String message) {
        String restApiId =
                Optional.ofNullable(System.getenv(apikey))
                        .orElseThrow(() -> new IllegalArgumentException(message));

        return String.format("https://%s.execute-api.eu-west-2.amazonaws.com", restApiId);
    }
}
