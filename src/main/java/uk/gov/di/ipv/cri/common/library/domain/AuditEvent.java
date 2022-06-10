package uk.gov.di.ipv.cri.common.library.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditEvent {

    @JsonProperty("timestamp")
    private final long timestamp;

    @JsonProperty("event_name")
    private final String event;

    @JsonCreator
    public AuditEvent(
            @JsonProperty(value = "timestamp", required = true) long timestamp,
            @JsonProperty(value = "event_name", required = true) String event) {
        this.timestamp = timestamp;
        this.event = event;
    }

    @JsonCreator
    public AuditEvent(
            @JsonProperty(value = "timestamp", required = true) long timestamp,
            @JsonProperty(value = "event_name", required = true) AuditEventType event) {
        this(timestamp, event.toString());
    }

    @Override
    public String toString() {
        return "AuditEvent{" + "timestamp=" + timestamp + ", event=" + event + '}';
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEvent() {
        return event;
    }
}
