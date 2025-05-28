package uk.gov.di.ipv.cri.common.library.helpers;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;
import uk.gov.di.ipv.cri.common.library.config.CriStubClientEnum;
import uk.gov.di.ipv.cri.common.library.config.Environment;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

public class SSMHelper {
    private final SsmClient ssmClient;
    private static final String COMMON_STACK_NAME =
            Environment.getEnvOrDefault("COMMON_STACK_NAME", "common-cri-api");
    private static final String CLIENT_ID =
            Environment.getEnvOrDefault("DEFAULT_CLIENT_ID", "ipv-core-stub-aws-headless");

    public SSMHelper() {
        ssmClient = new ClientProviderFactory().getSsmClient();
    }

    public String getParameterValue(CriStubClientEnum key) {
        String envOverride = Environment.getEnvOrDefault(key.getConfigName().toUpperCase(), null);
        return (envOverride != null ? envOverride : getParameterValue(getSsmPath(key))).trim();
    }

    public String getParameterValue(String path) {
        return getParameterValueByName(path).trim();
    }

    private String getParameterValueByName(String parameterName) {
        try {
            GetParameterRequest parameterRequest =
                    GetParameterRequest.builder().name(parameterName).build();

            GetParameterResponse parameterResponse = ssmClient.getParameter(parameterRequest);
            return parameterResponse.parameter().value();
        } catch (SsmException e) {
            e.getMessage();
            throw e;
        }
    }

    private String getSsmPath(CriStubClientEnum key) {
        return String.format(
                "/%s/clients/%s/jwtAuthentication/%s",
                COMMON_STACK_NAME, CLIENT_ID, key.getConfigName());
    }
}
