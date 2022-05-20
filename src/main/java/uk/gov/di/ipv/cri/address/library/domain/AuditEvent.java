package uk.gov.di.ipv.cri.address.library.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditEvent {

    @JsonProperty("timestamp")
    private final long timestamp;

    @JsonProperty("event_name")
    private final AuditEventTypes event;

    @JsonCreator
    public AuditEvent(
            @JsonProperty(value = "timestamp", required = true) long timestamp,
            @JsonProperty(value = "event_name", required = true) AuditEventTypes event) {
        this.timestamp = timestamp;
        this.event = event;
    }

    @Override
    public String toString() {
        return "AuditEvent{" + "timestamp=" + timestamp + ", event=" + event + '}';
    }

    public long getTimestamp() {
        return timestamp;
    }

    public AuditEventTypes getEvent() {
        return event;
    }
}
