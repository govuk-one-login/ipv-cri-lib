package uk.gov.di.ipv.cri.common.library.client;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.regions.Region;
import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class CommonApiClient {
    private final HttpClient httpClient;
    private final ClientConfigurationService clientConfigurationService;

    private static final String STACK_NAME = "test-resources";
    private static final String ENV = System.getenv("Environment");
    private static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
    private static final String AWS_SESSION_TOKEN = System.getenv("AWS_SESSION_TOKEN");
    private static final String AWS_SERVICE = "execute-api";
    private static final Region REGION = Region.EU_WEST_2;

    public CommonApiClient(ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;
        this.httpClient = HttpClient.newBuilder().build();
    }

    private static final String JSON_MIME_MEDIA_TYPE = "application/json";

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

    public HttpResponse<String> sendEventRequest(String sessionId)
            throws IOException, InterruptedException {

        URI eventsEndpointURI =
                URI.create(
                        "https://"
                                + STACK_NAME
                                + ".review-a."
                                + ENV
                                + ".account.gov.uk/events?partitionKey=SESSION%23"
                                + sessionId
                                + "&sortKey=TXMA%23IPV_HMRC_RECORD_CHECK_CRI_START");

        return sendRequest(
                signRequest(
                        SdkHttpFullRequest.builder()
                                .method(SdkHttpMethod.GET)
                                .uri(eventsEndpointURI)
                                .build(),
                        AwsSessionCredentials.create(
                                AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)));
    }

    private SdkHttpRequest signRequest(
            SdkHttpFullRequest unsignedRequest, AwsSessionCredentials credentials) {
        AwsV4HttpSigner signer = AwsV4HttpSigner.create();
        SignedRequest signedRequest =
                signer.sign(
                        r ->
                                r.identity(credentials)
                                        .request(unsignedRequest)
                                        .putProperty(
                                                AwsV4HttpSigner.SERVICE_SIGNING_NAME, AWS_SERVICE)
                                        .putProperty(AwsV4HttpSigner.REGION_NAME, REGION.id()));

        return signedRequest.request();
    }

    private HttpResponse<String> sendRequest(SdkHttpRequest sdkHttpRequest)
            throws IOException, InterruptedException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        requestBuilder.GET();
        requestBuilder.uri(sdkHttpRequest.getUri());
        for (Map.Entry<String, List<String>> entry : sdkHttpRequest.headers().entrySet()) {
            for (String value : entry.getValue()) {
                if (entry.getKey().equalsIgnoreCase("host")) {
                    continue;
                }
                requestBuilder.header(entry.getKey(), value);
            }
        }
        return sendHttpRequest(requestBuilder.build());
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

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
