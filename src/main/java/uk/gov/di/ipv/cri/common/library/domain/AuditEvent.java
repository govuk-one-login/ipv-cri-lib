package uk.gov.di.ipv.cri.common.library.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent<T> {

    @JsonProperty("timestamp")
    private final long timestamp;

    @JsonProperty("event_name")
    private final String event;

    @JsonProperty("component_id")
    private final String issuer;

    private PersonIdentityDetailed restricted;
    private AuditEventUser user;
    private T extensions;

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

    public PersonIdentityDetailed getRestricted() {
        return restricted;
    }

    public void setRestricted(PersonIdentityDetailed restricted) {
        this.restricted = restricted;
    }

    public T getExtensions() {
        return extensions;
    }

    public void setExtensions(T extensions) {
        this.extensions = extensions;
    }

    public AuditEventUser getUser() {
        return user;
    }

    public void setUser(AuditEventUser user) {
        this.user = user;
    }
}
