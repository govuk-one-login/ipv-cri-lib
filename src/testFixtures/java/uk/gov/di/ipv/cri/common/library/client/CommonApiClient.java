package uk.gov.di.ipv.cri.common.library.client;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import uk.gov.di.ipv.cri.common.library.helpers.SSMHelper;
import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static uk.gov.di.ipv.cri.common.library.client.HttpHeaders.JSON_MIME_MEDIA_TYPE;
import static uk.gov.di.ipv.cri.common.library.config.CriStubClientEnum.REDIRECT_URI;

public class CommonApiClient {
    private final HttpClient httpClient;
    private final ClientConfigurationService clientConfigurationService;
    private final SSMHelper ssmHelper;

    public CommonApiClient(
            ClientConfigurationService clientConfigurationService, SSMHelper ssmHelper) {
        this.clientConfigurationService = clientConfigurationService;
        this.ssmHelper = ssmHelper;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public HttpResponse<String> sendAuthorizationRequest(String sessionId)
            throws IOException, InterruptedException {
        var url =
                new URIBuilder(clientConfigurationService.getPrivateApiEndpoint())
                        .setPath(clientConfigurationService.createUriPath("authorization"))
                        .addParameter("redirect_uri", ssmHelper.getParameterValue(REDIRECT_URI))
                        .addParameter("client_id", clientConfigurationService.getDefaultClientId())
                        .addParameter("response_type", "code")
                        .addParameter("scope", "openid")
                        .addParameter("state", "state-ipv")
                        .build();

        return sendHttpRequest(
                HttpRequest.newBuilder()
                        .uri(url)
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.SESSION_ID, sessionId)
                        .GET()
                        .build());
    }

    public HttpResponse<String> sendTokenRequest(PrivateKeyJWT privateKeyJwt, String code)
            throws IOException, InterruptedException, URISyntaxException {
        var authorisationGrant =
                new AuthorizationCodeGrant(
                        new AuthorizationCode(code),
                        new URI(ssmHelper.getParameterValue(REDIRECT_URI)));

        var tokenRequestBody =
                new TokenRequest(getPublicApiUrl("token"), privateKeyJwt, authorisationGrant)
                        .toHTTPRequest();

        return sendHttpRequest(
                HttpRequest.newBuilder()
                        .uri(getPublicApiUrl("token"))
                        .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                        .header(HttpHeaders.API_KEY, clientConfigurationService.getPublicApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody.getBody()))
                        .build());
    }

    public HttpResponse<String> sendSessionRequest(String sessionRequestBody)
            throws IOException, InterruptedException {
        return sendSessionRequest(sessionRequestBody, Map.of("X-Forwarded-For", "192.168.0.1"));
    }

    public HttpResponse<String> sendNewSessionRequest(String sessionRequestBody)
            throws IOException, InterruptedException {
        return sendSessionRequest(
                sessionRequestBody,
                Map.of(
                        "txma-audit-encoded",
                        "deviceInformation",
                        "X-Forwarded-For",
                        "192.168.0.1"));
    }

    private HttpResponse<String> sendSessionRequest(
            String body, Map<String, String> additionalHeaders)
            throws IOException, InterruptedException {

        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(getPrivateApiUrl("session"))
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE);

        additionalHeaders.forEach(builder::header);

        HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();

        return sendHttpRequest(request);
    }

    private URI getPublicApiUrl(String path) {
        return buildApiUri(clientConfigurationService.getPublicApiEndpoint(), path);
    }

    private URI getPrivateApiUrl(String path) {
        return buildApiUri(clientConfigurationService.getPrivateApiEndpoint(), path);
    }

    private URI buildApiUri(String baseUrl, String path) {
        return new URIBuilder(baseUrl)
                .setPath(clientConfigurationService.createUriPath(path))
                .build();
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
