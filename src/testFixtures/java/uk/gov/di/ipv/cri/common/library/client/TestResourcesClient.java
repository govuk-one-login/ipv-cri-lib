package uk.gov.di.ipv.cri.common.library.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TestResourcesClient {
    private final String testHarnessUrl;

    private static final SdkHttpClient CLIENT = AwsCrtHttpClient.builder().build();
    private static final AwsV4HttpSigner SIGNER = AwsV4HttpSigner.create();
    private static final ObjectMapper mapper = new ObjectMapper();

    public TestResourcesClient(ClientConfigurationService client) {
        this.testHarnessUrl =
                CloudFormationHelper.getOutput(
                        client.getTestResourcesStackName(), "TestHarnessExecuteUrl");
    }

    public String getTestHarnessUrl() {
        return this.testHarnessUrl;
    }

    public static String getResponseBody(HttpExecuteResponse response) throws IOException {
        if (response.responseBody().isEmpty()) {
            return null;
        }

        try (InputStream bodyStream = response.responseBody().get()) {
            return IoUtils.toUtf8String(bodyStream);
        }
    }

    public HttpResponse<String> sendOverwrittenStartRequest(String claimOverrides)
            throws IOException, InterruptedException {
        return postClaimsSetOverrideToStart(buildStartRequestBody(claimOverrides));
    }

    public HttpResponse<String> sendOverwrittenStartRequest(Map<String, Object> claimOverrides)
            throws IOException, InterruptedException {
        return postClaimsSetOverrideToStart(buildStartRequestBody(claimOverrides));
    }

    private HttpResponse<String> postClaimsSetOverrideToStart(String requestBody)
            throws IOException, InterruptedException {
        return sendSignedStartRequest(
                SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.POST)
                        .uri(new URIBuilder(getTestHarnessUrl()).setPath("start").build())
                        .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaders.JSON_MIME_MEDIA_TYPE)
                        .contentStreamProvider(ContentStreamProvider.fromUtf8String(requestBody))
                        .build(),
                requestBody);
    }

    public HttpResponse<String> sendStartRequest() throws IOException, InterruptedException {
        final String requestBody = "{}";
        final URI startEndpointURI = new URIBuilder(getTestHarnessUrl()).setPath("start").build();
        final SdkHttpFullRequest request =
                SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.POST)
                        .uri(startEndpointURI)
                        .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaders.JSON_MIME_MEDIA_TYPE)
                        .contentStreamProvider(
                                RequestBody.fromString(requestBody).contentStreamProvider())
                        .build();

        return sendSignedStartRequest(request, requestBody);
    }

    public HttpExecuteResponse sendEventRequest(String sessionId, String eventName)
            throws IOException {
        final URI eventsEndpointURI =
                new URIBuilder(getTestHarnessUrl())
                        .setPath("events")
                        .addParameter("partitionKey", encodePrefixedKey("SESSION#", sessionId))
                        .addParameter("sortKey", encodePrefixedKey("TXMA#", eventName))
                        .build();

        return sendRequest(
                SdkHttpFullRequest.builder()
                        .method(SdkHttpMethod.GET)
                        .uri(eventsEndpointURI)
                        .build());
    }

    private String encodePrefixedKey(String prefix, String sessionId) {
        return URLEncoder.encode(String.format("%s%s", prefix, sessionId), StandardCharsets.UTF_8);
    }

    private HttpResponse<String> sendSignedStartRequest(
            SdkHttpFullRequest unsignedRequest, String requestBody)
            throws IOException, InterruptedException {
        final SignedRequest signedRequest = signRequest(unsignedRequest);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder request =
                HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(requestBody));
        request.uri(unsignedRequest.getUri());
        signedRequest
                .request()
                .headers()
                .forEach(
                        (key, value) -> {
                            if (!key.startsWith("Host")) {
                                request.header(key, value.get(0));
                            }
                        });
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
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
                    signRequest -> {
                        signRequest
                                .request(unsignedRequest)
                                .payload(unsignedRequest.contentStreamProvider().orElse(null))
                                .identity(credentialsProvider.resolveCredentials())
                                .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "execute-api")
                                .putProperty(AwsV4HttpSigner.REGION_NAME, Region.EU_WEST_2.id());
                    });
        }
    }

    private static String buildStartRequestBody(String claimOverridesFile) throws IOException {
        Map<String, Object> claimSet = readJsonFromFile(claimOverridesFile);

        return buildStartRequestBody(claimSet);
    }

    private static String buildStartRequestBody(Map<String, Object> claimSet)
            throws JsonProcessingException {
        JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();

        if (claimSet != null) {
            for (Map.Entry<String, Object> claims : claimSet.entrySet()) {
                claimsSetBuilder.claim(claims.getKey(), claims.getValue());
            }
        }

        JWTClaimsSet jwtClaimsSet = claimsSetBuilder.build();
        return mapper.writeValueAsString(jwtClaimsSet.toJSONObject());
    }

    private static Map<String, Object> readJsonFromFile(String overridesFileName)
            throws IOException {
        if (overridesFileName.trim().isEmpty()) {
            throw new FileNotFoundException("No file provided.");
        }

        InputStream input =
                TestResourcesClient.class
                        .getClassLoader()
                        .getResourceAsStream("overrides/" + overridesFileName);
        if (input == null) {
            throw new FileNotFoundException(
                    "Override JSON file not found: overrides/" + overridesFileName);
        }

        return mapper.readValue(input, new TypeReference<>() {});
    }
}
