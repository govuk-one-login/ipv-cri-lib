package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.common.library.domain.RawAuditEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawAuditEventTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void testRawEventDataIsSetWithTestProperties() throws JsonProcessingException {
        final String json = "{\"S\":\"sampleData\", \"test\":\"test\"}";
        final RawAuditEvent rawAuditEvent = OBJECT_MAPPER.readValue(json, RawAuditEvent.class);

        assertEquals("sampleData", rawAuditEvent.getData());
    }

    @Test
    void rawEventDataIsReturnedWhenSet() {
        final RawAuditEvent rawAuditEvent = new RawAuditEvent();
        rawAuditEvent.setData("newData");

        assertEquals("newData", rawAuditEvent.getData());
    }
}
