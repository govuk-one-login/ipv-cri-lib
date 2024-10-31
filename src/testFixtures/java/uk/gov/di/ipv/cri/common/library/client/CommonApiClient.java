package uk.gov.di.ipv.cri.common.library.client;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.regions.Region;
import uk.gov.di.ipv.cri.common.library.aws.CloudFormationHelper;
import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class CommonApiClient {
    private final HttpClient httpClient;
    private final String testHarnessUrl;
    private final ClientConfigurationService clientConfigurationService;

    private static final String JSON_MIME_MEDIA_TYPE = "application/json";

    public CommonApiClient(ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;
        this.httpClient = HttpClient.newBuilder().build();

        this.testHarnessUrl =
                CloudFormationHelper.getOutput(
                        clientConfigurationService.getTestResourcesStackName(),
                        "TestHarnessExecuteUrl");
    }

    public HttpResponse<String> sendAuthorizationRequest(String sessionId)
            throws IOException, InterruptedException {
        var url =
                new URIBuilder(this.clientConfigurationService.getPrivateApiEndpoint())
                        .setPath(this.clientConfigurationService.createUriPath("authorization"))
                        .addParameter(
                                "redirect_uri",
                                new URIBuilder(this.clientConfigurationService.getIPVCoreStubURL())
                                        .setPath("/callback")
                                        .build()
                                        .toString())
                        .addParameter(
                                "client_id", this.clientConfigurationService.getDefaultClientId())
                        .addParameter("response_type", "code")
                        .addParameter("scope", "openid")
                        .addParameter("state", "state-ipv")
                        .build();

        var request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .GET()
                        .build();
        return sendHttpRequest(request);
    }

    public HttpResponse<String> sendSessionRequest(String sessionRequestBody)
            throws IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPrivateApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "session"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .header("X-Forwarded-For", "192.168.0.1")
                        .POST(HttpRequest.BodyPublishers.ofString(sessionRequestBody))
                        .build();
        return sendHttpRequest(request);
    }

    public HttpResponse<String> sendNewSessionRequest(String sessionRequestBody)
            throws IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPrivateApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "session"))
                                        .build())
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .header("X-Forwarded-For", "192.168.0.1")
                        .header("txma-audit-encoded", "deviceInformation")
                        .POST(HttpRequest.BodyPublishers.ofString(sessionRequestBody))
                        .build();
        return sendHttpRequest(request);
    }

    public HttpResponse<String> sendTokenRequest(String privateKeyJwt)
            throws IOException, InterruptedException {
        var request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(
                                                this.clientConfigurationService
                                                        .getPublicApiEndpoint())
                                        .setPath(
                                                this.clientConfigurationService.createUriPath(
                                                        "token"))
                                        .build())
                        .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                        .header(
                                HttpHeaders.API_KEY,
                                this.clientConfigurationService.getPublicApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(privateKeyJwt))
                        .build();
        return sendHttpRequest(request);
    }

    public HttpResponse<String> sendEventRequest(String sessionId)
            throws IOException, InterruptedException {
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

        final SdkHttpRequest request =
                SdkHttpRequest.builder().method(SdkHttpMethod.GET).uri(eventsEndpointURI).build();

        return sendSignedRequest(request);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendSignedRequest(SdkHttpRequest unsignedRequest)
            throws IOException, InterruptedException {
        final SdkHttpRequest signedRequest = signRequest(unsignedRequest);

        final HttpRequest.Builder httpRequest =
                HttpRequest.newBuilder().GET().uri(signedRequest.getUri());

        signedRequest.headers().entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase("host"))
                .forEach(
                        entry ->
                                entry.getValue()
                                        .forEach(
                                                value ->
                                                        httpRequest.header(entry.getKey(), value)));

        return sendHttpRequest(httpRequest.build());
    }

    private SdkHttpRequest signRequest(SdkHttpRequest unsignedRequest) {
        try (DefaultCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create()) {
            return AwsV4HttpSigner.create()
                    .sign(
                            signRequest ->
                                    signRequest
                                            .request(unsignedRequest)
                                            .identity(credentialsProvider.resolveCredentials())
                                            .putProperty(
                                                    AwsV4HttpSigner.SERVICE_SIGNING_NAME,
                                                    "execute-api")
                                            .putProperty(
                                                    AwsV4HttpSigner.REGION_NAME,
                                                    Region.EU_WEST_2.id()))
                    .request();
        }
    }
}
