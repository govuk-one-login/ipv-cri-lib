package uk.gov.di.ipv.cri.common.library.stepdefinitions;

import software.amazon.awssdk.http.HttpExecuteResponse;

import java.net.http.HttpResponse;

public class CriTestContext {
    private HttpExecuteResponse testHarnessResponse;
    private HttpResponse<String> response;
    private String sessionId;
    private String accessToken;
    private String serialisedUserIdentity;

    public HttpResponse<String> getResponse() {
        return response;
    }

    public HttpExecuteResponse getTestHarnessResponse() {
        return testHarnessResponse;
    }

    public void setResponse(HttpResponse<String> response) {
        this.response = response;
    }

    public void setResponse(HttpExecuteResponse response) {
        this.testHarnessResponse = response;
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

    public String getSerialisedUserIdentity() {
        return serialisedUserIdentity;
    }

    public void setSerialisedUserIdentity(String serialisedUserIdentity) {
        this.serialisedUserIdentity = serialisedUserIdentity;
    }
}
