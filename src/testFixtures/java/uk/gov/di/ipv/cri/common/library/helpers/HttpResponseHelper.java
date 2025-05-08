package uk.gov.di.ipv.cri.common.library.helpers;

import java.net.http.HttpResponse;

public class HttpResponseHelper {
    private HttpResponse<String> response;

    public void setResponse(HttpResponse<String> response) {
        this.response = response;
    }

    public HttpResponse<String> getResponse() {
        return this.response;
    }
}
