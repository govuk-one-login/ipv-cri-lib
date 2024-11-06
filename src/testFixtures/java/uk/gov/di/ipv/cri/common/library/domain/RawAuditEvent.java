package uk.gov.di.ipv.cri.common.library.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawAuditEvent {
    @JsonProperty("S")
    private String data;

    public RawAuditEvent(String data) {
        this.data = data;
    }

    public RawAuditEvent() {}

    public String getData() {
        return data;
    }

    public void setData(String s) {
        this.data = s;
    }
}
