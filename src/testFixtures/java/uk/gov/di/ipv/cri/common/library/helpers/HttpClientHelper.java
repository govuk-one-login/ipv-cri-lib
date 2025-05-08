package uk.gov.di.ipv.cri.common.library.helpers;

import uk.gov.di.ipv.cri.common.library.config.EnvironmentConfig;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClientHelper {
    private final HttpClient httpClient;

    public HttpClientHelper() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    public HttpResponse<String> sendHttpRequest(HttpRequest request)
            throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public String createUriPath(String path) {
        return String.format("/%s/%s", EnvironmentConfig.getEnvironment("ENVIRONMENT"), path);
    }
}
