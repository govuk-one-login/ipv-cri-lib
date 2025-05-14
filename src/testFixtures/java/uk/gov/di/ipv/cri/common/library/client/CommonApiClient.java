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

public class CommonApiClient {
    private final HttpClient httpClient;
    private final ClientConfigurationService clientConfigurationService;
    private final SSMHelper ssmHelper;

    private static final String JSON_MIME_MEDIA_TYPE = "application/json";

    public CommonApiClient(
            ClientConfigurationService clientConfigurationService, SSMHelper ssmHelper) {
        this.clientConfigurationService = clientConfigurationService;
        this.ssmHelper = ssmHelper;
        this.httpClient = HttpClient.newBuilder().build();
    }

    public HttpResponse<String> sendAuthorizationRequest(String sessionId)
            throws IOException, InterruptedException {
        var url =
                new URIBuilder(this.clientConfigurationService.getPrivateApiEndpoint())
                        .setPath(this.clientConfigurationService.createUriPath("authorization"))
                        .addParameter(
                                "redirect_uri",
                                ssmHelper
                                        .getParameterValueByName(
                                                "/"
                                                        + this.clientConfigurationService
                                                                .getCommonStackName()
                                                        + "/clients/"
                                                        + this.clientConfigurationService
                                                                .getDefaultClientId()
                                                        + "/jwtAuthentication/redirectUri")
                                        .trim())
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

    public HttpResponse<String> sendTokenRequest(PrivateKeyJWT privateKeyJwt, String code)
            throws IOException, InterruptedException, URISyntaxException {
        var authorisationGrant =
                new AuthorizationCodeGrant(
                        new AuthorizationCode(code),
                        new URI(
                                ssmHelper
                                        .getParameterValueByName(
                                                "/"
                                                        + this.clientConfigurationService
                                                                .getCommonStackName()
                                                        + "/clients/"
                                                        + this.clientConfigurationService
                                                                .getDefaultClientId()
                                                        + "/jwtAuthentication/redirectUri")
                                        .trim()));
        var tokenRequest =
                new TokenRequest(
                        new URIBuilder(this.clientConfigurationService.getPublicApiEndpoint())
                                .setPath(this.clientConfigurationService.createUriPath("token"))
                                .build(),
                        privateKeyJwt,
                        authorisationGrant);

        var tokenRequestBody = tokenRequest.toHTTPRequest();

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
                        .POST(HttpRequest.BodyPublishers.ofString(tokenRequestBody.getBody()))
                        .build();
        return sendHttpRequest(request);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
