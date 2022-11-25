package uk.gov.di.ipv.cri.common.library.client;

public final class HttpHeaders {
    private HttpHeaders() {
        // do not allow instantiation
    }

    public static final String ACCEPT = "Accept";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String SESSION_ID = "session-id";
    public static final String API_KEY = "x-api-key"; // pragma: allowlist secret
}
