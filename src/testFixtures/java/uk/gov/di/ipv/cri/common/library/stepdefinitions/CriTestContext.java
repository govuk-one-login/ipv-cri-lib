package uk.gov.di.ipv.cri.common.library.stepdefinitions;

import software.amazon.awssdk.http.HttpExecuteResponse;
import uk.gov.di.ipv.cri.common.library.client.TestResourcesClient;

import java.io.IOException;
import java.net.http.HttpResponse;

public class CriTestContext {
    private HttpExecuteResponse testHarnessResponse;
    private HttpResponse<String> response;
    private String sessionId;
    private String accessToken;
    private String testHarnessResponseBody;

    public HttpResponse<String> getResponse() {
        return response;
    }

    public HttpExecuteResponse getTestHarnessResponse() {
        return testHarnessResponse;
    }

    public String getTestHarnessResponseBody() {
        return testHarnessResponseBody;
    }

    public void setResponse(HttpResponse<String> response) {
        this.response = response;
    }

    public void setResponse(HttpExecuteResponse response) throws IOException {
        this.testHarnessResponse = response;
        this.testHarnessResponseBody = TestResourcesClient.getResponseBody(response);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

}
