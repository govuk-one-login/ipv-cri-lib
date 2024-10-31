package uk.gov.di.ipv.cri.common.library.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestHarnessResponse<T> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("event")
    private RawAuditEvent rawAuditEvent;

    public TestHarnessResponse(RawAuditEvent rawAuditEvent) {
        this.rawAuditEvent = rawAuditEvent;
    }

    public TestHarnessResponse() {}

    public RawAuditEvent getEvent() {
        return rawAuditEvent;
    }

    public AuditEvent<T> readAuditEvent() throws IOException {
        return OBJECT_MAPPER.readValue(rawAuditEvent.getData(), new TypeReference<>() {});
    }

    public void setEvent(RawAuditEvent rawAuditEvent) {
        this.rawAuditEvent = rawAuditEvent;
    }
}
