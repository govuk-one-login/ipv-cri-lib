package uk.gov.di.ipv.cri.common.library.config;

public enum CriStubClientEnum {
    ISSUER("issuer"),
    AUDIENCE("audience"),
    REDIRECT_URI("redirectUri"),
    AUTHENTICATION_ALG("authenticationAlg"),
    PUBLIC_SIGNING_JWK_BASE64("publicSigningJwkBase64");

    private static final String CLIENT_PATH_TEMPLATE = "/%s/clients/%s/jwtAuthentication/%s";
    private static final String COMMON_STACK_NAME =
            EnvironConfig.getEnvOrDefault("COMMON_STACK_NAME", "common-cri-api");
    private static final String CLIENT_ID =
            EnvironConfig.getEnvOrDefault("CLIENT_ID", "ipv-core-stub-aws-headless");

    private final String configName;

    CriStubClientEnum(String configName) {
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    public String getSsmPath() {
        return String.format(CLIENT_PATH_TEMPLATE, COMMON_STACK_NAME, CLIENT_ID, configName);
    }
}
