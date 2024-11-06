package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.RawAuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.TestHarnessResponse;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestHarnessResponseTest {
    @Mock private RawAuditEvent mockRawAuditEvent;

    @Test
    void testConstructorAndGetter() {
        final var response =
                new TestHarnessResponse<AuditEvent<Map<String, Object>>>(mockRawAuditEvent);

        assertEquals(
                mockRawAuditEvent,
                response.getEvent(),
                "Constructor should correctly set rawEvent");
    }

    @Test
    void testDefaultConstructorAndSetter() {
        final var response = new TestHarnessResponse<AuditEvent<Map<String, Object>>>();
        response.setEvent(mockRawAuditEvent);

        assertEquals(
                mockRawAuditEvent, response.getEvent(), "Setter should correctly set rawEvent");
    }

    @Test
    void sendValidDataToReadAuditEventsCorrectly() throws IOException {
        final var response =
                new TestHarnessResponse<AuditEvent<Map<String, Object>>>(mockRawAuditEvent);

        final String jsonData =
                "{\"timestamp\": \"1730463762\", \"event_timestamp_ms\": \"1730463762900\", \"event_name\": \"IPV_ADDRESS_CRI_START\", \"component_id\": \"https://review-a.dev.account.gov.uk\"}";

        when(mockRawAuditEvent.getData()).thenReturn(jsonData);
        AuditEvent<AuditEvent<Map<String, Object>>> auditEvent = response.readAuditEvent();

        assertNotNull(auditEvent, "readAuditEvent should return a non-null AuditEvent object");
    }

    @Test
    void sendInvalidDataToReadAuditEventThrowIOException() {
        final var response =
                new TestHarnessResponse<AuditEvent<Map<String, Object>>>(mockRawAuditEvent);

        when(mockRawAuditEvent.getData()).thenReturn("{ invalid json }");

        assertThrows(
                IOException.class,
                response::readAuditEvent,
                "Invalid JSON should throw IOException");
    }

    @Test
    void sendNoRawEventToReadAuditEventThrowNullPointerException() {
        final var response = new TestHarnessResponse<AuditEvent<Map<String, Object>>>();

        assertThrows(
                NullPointerException.class,
                response::readAuditEvent,
                "No rawEvent should throw NullPointerException");
    }
}
