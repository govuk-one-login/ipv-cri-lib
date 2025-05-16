package uk.gov.di.ipv.cri.common.library.helpers;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

public class SSMHelper {
    SsmClient ssmClient;

    public SSMHelper() {
        ssmClient = new ClientProviderFactory().getSsmClient();
    }

    public SSMHelper(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
    }

    public String getParameterValueByName(String parameterName) {
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
}
