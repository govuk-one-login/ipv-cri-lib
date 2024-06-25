package uk.gov.di.ipv.cri.common.library.client;

import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class IpvCoreStubClient {
    private static final String JSON_MIME_MEDIA_TYPE = "application/json";

    private final HttpClient httpClient;
    private final ClientConfigurationService clientConfigurationService;

    public IpvCoreStubClient(ClientConfigurationService clientConfigurationService) {
        this.clientConfigurationService = clientConfigurationService;

        this.httpClient =
                HttpClient.newBuilder()
                        .authenticator(
                                new Authenticator() {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(
                                                clientConfigurationService.getIpvCoreStubUsername(),
                                                clientConfigurationService
                                                        .getIpvCoreStubPassword()
                                                        .toCharArray());
                                    }
                                })
                        .build();
    }

    public String getClaimsForUser(int userDataRowNumber) throws IOException, InterruptedException {
        URI uri =
                new URIBuilder(this.clientConfigurationService.getIPVCoreStubURL())
                        .setPath("/backend/generateInitialClaimsSet")
                        .addParameter("cri", clientConfigurationService.getIpvCoreStubCriId())
                        .addParameter("rowNumber", String.valueOf(userDataRowNumber))
                        .build();
        HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
        return sendHttpRequest(request).body();
    }

    public String getClaimsForUserWithEvidenceRequested(
            int userDataRowNumber, int verificationScore, int strengthScore)
            throws IOException, InterruptedException {
        URI uri =
                new URIBuilder(this.clientConfigurationService.getIPVCoreStubURL())
                        .setPath("/backend/generateInitialClaimsSet")
                        .addParameter("cri", clientConfigurationService.getIpvCoreStubCriId())
                        .addParameter("rowNumber", String.valueOf(userDataRowNumber))
                        .addParameter("scoringPolicy", "gpg45")
                        .addParameter("verificationScore", String.valueOf(verificationScore))
                        .addParameter("strengthScore", String.valueOf(strengthScore))
                        .build();
        HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
        return sendHttpRequest(request).body();
    }

    public String createSessionRequest(String requestBody)
            throws IOException, InterruptedException {

        var uri =
                new URIBuilder(this.clientConfigurationService.getIPVCoreStubURL())
                        .setPath("/backend/createSessionRequest")
                        .addParameter("cri", clientConfigurationService.getIpvCoreStubCriId())
                        .build();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                        .header(HttpHeaders.CONTENT_TYPE, JSON_MIME_MEDIA_TYPE)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        return sendHttpRequest(request).body();
    }

    public String getPrivateKeyJWTFormParamsForAuthCode(String authorizationCode)
            throws IOException, InterruptedException {
        var url =
                new URIBuilder(this.clientConfigurationService.getIPVCoreStubURL())
                        .setPath("/backend/createTokenRequestPrivateKeyJWT")
                        .addParameter("cri", clientConfigurationService.getIpvCoreStubCriId())
                        .addParameter("authorization_code", authorizationCode)
                        .build();

        HttpRequest request = HttpRequest.newBuilder().uri(url).GET().build();
        return sendHttpRequest(request).body();
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
