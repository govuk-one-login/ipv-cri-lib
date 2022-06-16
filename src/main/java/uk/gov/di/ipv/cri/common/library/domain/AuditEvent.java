package uk.gov.di.ipv.cri.common.library.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditEvent {

    @JsonProperty("timestamp")
    private final long timestamp;

    @JsonProperty("event_name")
    private final String event;

    @JsonProperty("component_id")
    private final String issuer;

    @JsonCreator
    public AuditEvent(
            @JsonProperty(value = "timestamp", required = true) long timestamp,
            @JsonProperty(value = "event_name", required = true) String event,
            @JsonProperty(value = "component_id", required = true) String issuer) {
        this.timestamp = timestamp;
        this.event = event;
        this.issuer = issuer;
    }

    @Override
    public String toString() {
        return "AuditEvent{"
                + "timestamp="
                + timestamp
                + ", event="
                + event
                + ", component_id="
                + issuer
                + '}';
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEvent() {
        return event;
    }
}
