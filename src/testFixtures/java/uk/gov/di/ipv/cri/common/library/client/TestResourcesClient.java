package uk.gov.di.ipv.cri.common.library.client;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;
import uk.gov.di.ipv.cri.common.library.aws.CloudFormationHelper;
import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TestResourcesClient {
    private final String testHarnessUrl;

    private static final SdkHttpClient CLIENT = AwsCrtHttpClient.builder().build();
    private static final AwsV4HttpSigner SIGNER = AwsV4HttpSigner.create();

    public TestResourcesClient(ClientConfigurationService clientConfigurationService) {
        this.testHarnessUrl =
                CloudFormationHelper.getOutput(
                        clientConfigurationService.getTestResourcesStackName(),
                        "TestHarnessExecuteUrl");
    }

    /** Convert the response body to a string and close its input stream */
    public static String getResponseBody(HttpExecuteResponse response) throws IOException {
        if (response.responseBody().isEmpty()) {
            return null;
        }

        try (InputStream bodyStream = response.responseBody().get()) {
            return IoUtils.toUtf8String(bodyStream);
        }
    }

    public HttpExecuteResponse sendEventRequest(String sessionId) throws IOException {
        final URI eventsEndpointURI =
                new URIBuilder(this.testHarnessUrl)
                        .setPath("events")
                        .addParameter(
                                "partitionKey",
                                URLEncoder.encode(
                                        String.format("%s%s", "SESSION#", sessionId),
                                        StandardCharsets.UTF_8))
                        .addParameter("sortKey", "TXMA")
                        .build();

        final SdkHttpFullRequest request =
                SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.GET)
                        .uri(eventsEndpointURI)
                        .build();

        return sendRequest(request);
    }

    private HttpExecuteResponse sendRequest(SdkHttpFullRequest unsignedRequest) throws IOException {
        final SignedRequest signedRequest = signRequest(unsignedRequest);

        final HttpExecuteRequest httpExecuteRequest =
                HttpExecuteRequest.builder()
                        .request(signedRequest.request())
                        .contentStreamProvider(signedRequest.payload().orElse(null))
                        .build();

        return CLIENT.prepareRequest(httpExecuteRequest).call();
    }

    private SignedRequest signRequest(SdkHttpFullRequest unsignedRequest) {
        try (DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create()) {
            return SIGNER.sign(
                    signRequest ->
                            signRequest
                                    .request(unsignedRequest)
                                    .identity(credentialsProvider.resolveCredentials())
                                    .putProperty(
                                            AwsV4HttpSigner.SERVICE_SIGNING_NAME, "execute-api")
                                    .putProperty(
                                            AwsV4HttpSigner.REGION_NAME, Region.EU_WEST_2.id()));
        }
    }
}
