package uk.gov.di.ipv.cri.common.library.config;

import uk.gov.di.ipv.cri.common.library.helpers.SSMHelper;

import java.util.HashMap;
import java.util.Map;

public class CriStubClientConfig {
    private final SSMHelper ssmHelper;
    private static final String COMMON_STACK_NAME =
            EnvironConfig.getEnvOrDefault("COMMON_STACK_NAME", "common-cri-api");
    private static final String CLIENT_ID =
            EnvironConfig.getEnvOrDefault("DEFAULT_CLIENT_ID", "ipv-core-stub-aws-headless");

    public CriStubClientConfig(SSMHelper ssmHelper) {
        this.ssmHelper = ssmHelper;
    }

    public String getValue(CriStubClientEnum key) {
        String envOverride = EnvironConfig.getEnvOrDefault(key.getConfigName().toUpperCase(), null);
        return envOverride != null ? envOverride : fetchFromSsm(getSsmPath(key));
    }

    private String fetchFromSsm(String path) {
        return ssmHelper.getParameterValueByName(path);
    }

    private String getSsmPath(CriStubClientEnum key) {
        return String.format(
                "/%s/clients/%s/jwtAuthentication/%s",
                COMMON_STACK_NAME, CLIENT_ID, key.getConfigName());
    }
}
