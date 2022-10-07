package uk.gov.di.ipv.cri.common.library.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum OauthErrorResponse {
    ACCESS_DENIED_ERROR("access_denied", "Authorization permission denied");

    private final String code;
    private final String message;

    OauthErrorResponse(
            @JsonProperty(required = true, value = "error") String code,
            @JsonProperty(required = true, value = "error_description") String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorSummary() {
        return getCode() + ": " + getMessage();
    }
}
