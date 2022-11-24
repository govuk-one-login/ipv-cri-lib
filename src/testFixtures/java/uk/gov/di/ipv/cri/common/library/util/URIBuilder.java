package uk.gov.di.ipv.cri.common.library.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class URIBuilder {
    private final Map<String, String> queryStringParameters;
    private final String baseUri;
    private String path;

    public URIBuilder(String baseUri) {
        if (isBlankOrEmpty(baseUri)) {
            throw new IllegalArgumentException("baseUri must not be null or empty");
        }
        this.baseUri = baseUri.endsWith("/") ? baseUri.substring(0, baseUri.length() - 1) : baseUri;
        this.queryStringParameters = new HashMap<>();
    }

    public URIBuilder setPath(String path) {
        if (isBlankOrEmpty(path)) {
            throw new IllegalArgumentException("path must not be null or empty");
        }
        this.path = path.startsWith("/") ? path : "/".concat(path);
        return this;
    }

    public URIBuilder addParameter(String name, String value) {
        if (isBlankOrEmpty(name)) {
            throw new IllegalArgumentException("name must not be null or empty");
        }
        if (isBlankOrEmpty(value)) {
            Objects.requireNonNull(value, "value must not be null or emtpy");
        }
        this.queryStringParameters.put(name, value);
        return this;
    }

    public URI build() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.baseUri);
        if (Objects.nonNull(this.path)) {
            sb.append(this.path);
        }
        if (!this.queryStringParameters.isEmpty()) {
            String[] queryStringParams =
                    this.queryStringParameters.entrySet().stream()
                            .map(
                                    keyValue ->
                                            String.format(
                                                    "%s=%s",
                                                    keyValue.getKey(), keyValue.getValue()))
                            .toArray(String[]::new);
            sb.append("?");
            sb.append(String.join("&", queryStringParams));
        }
        return URI.create(sb.toString());
    }

    private boolean isBlankOrEmpty(String input) {
        return Objects.isNull(input) || (input.isEmpty() || input.isBlank());
    }
}
