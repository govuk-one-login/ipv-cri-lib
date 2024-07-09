package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    private static final String SQS_QUEUE_URL = "https://example-queue-url";

    @Mock private SqsClient mockSqs;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private ObjectMapper mockObjectMapper;
    @Mock private AuditEventFactory mockAuditEventFactory;
    private AuditService auditService;

    @BeforeEach
    void setup() {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(SQS_QUEUE_URL);
        when(mockObjectMapper.registerModule(any())).thenReturn(mockObjectMapper);
        auditService =
                new AuditService(
                        mockSqs, mockConfigurationService, mockObjectMapper, mockAuditEventFactory);
    }

    @Test
    void shouldSendMessageToSqsQueue(@Mock AuditEvent<Object> auditEvent)
            throws JsonProcessingException, SqsException {
        ArgumentCaptor<SendMessageRequest> sqsSendMessageRequestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        String serialisedAuditEvent = "serialised auditEvent";

        when(mockAuditEventFactory.create(AuditEventType.START.toString(), null, null))
                .thenReturn(auditEvent);
        when(mockObjectMapper.writeValueAsString(auditEvent)).thenReturn(serialisedAuditEvent);

        SendMessageResponse mockSendMessageResponse = mock(SendMessageResponse.class);
        when(mockSqs.sendMessage(sqsSendMessageRequestCaptor.capture()))
                .thenReturn(mockSendMessageResponse);

        auditService.sendAuditEvent(AuditEventType.START);
        SendMessageRequest capturedValue = sqsSendMessageRequestCaptor.getValue();
        verify(mockSqs).sendMessage(capturedValue);
        verify(mockAuditEventFactory).create("START", null, null);
        verify(mockObjectMapper).writeValueAsString(auditEvent);
        assertEquals(serialisedAuditEvent, capturedValue.messageBody());
        assertEquals(SQS_QUEUE_URL, capturedValue.queueUrl());
    }

    @Test
    void shouldSendAuditEventWithContext(@Mock AuditEvent<Object> auditEvent)
            throws SqsException, JsonProcessingException {
        AuditEventContext auditEventContext =
                new AuditEventContext(
                        mock(PersonIdentityDetailed.class),
                        Map.of("test-header-name", "test-header-value"),
                        new SessionItem());
        when(mockAuditEventFactory.create(AuditEventType.START.toString(), auditEventContext, null))
                .thenReturn(auditEvent);
        when(mockObjectMapper.writeValueAsString(auditEvent)).thenReturn("serialised audit event");

        auditService.sendAuditEvent(AuditEventType.START, auditEventContext);

        verify(mockAuditEventFactory)
                .create(AuditEventType.START.toString(), auditEventContext, null);
        verify(mockSqs).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldSendAuditEventWithExtensionsData(@Mock AuditEvent<Map<String, Object>> auditEvent)
            throws SqsException, JsonProcessingException {
        Map<String, Object> extensionsDataEntries = Map.of("test", "value");
        when(mockAuditEventFactory.create(
                        AuditEventType.START.toString(), null, extensionsDataEntries))
                .thenReturn(auditEvent);
        when(mockObjectMapper.writeValueAsString(auditEvent)).thenReturn("serialised audit event");
        auditService.sendAuditEvent(AuditEventType.START, extensionsDataEntries);
        verify(mockAuditEventFactory)
                .create(AuditEventType.START.toString(), null, extensionsDataEntries);
        verify(mockObjectMapper).writeValueAsString(auditEvent);
        verify(mockSqs).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldThrowSqsExceptionWhenSerialisationFails(@Mock AuditEvent<Object> auditEvent)
            throws JsonProcessingException {
        String audiEventType = "CUSTOM_AUDIT_EVENT_TYPE";
        when(mockAuditEventFactory.create(audiEventType, null, null)).thenReturn(auditEvent);
        when(mockObjectMapper.writeValueAsString(auditEvent))
                .thenThrow(mock(JsonProcessingException.class));

        assertThrows(
                SqsException.class, () -> auditService.sendAuditEvent("CUSTOM_AUDIT_EVENT_TYPE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void ConstructorShouldThrowErrorWhenNoQueueUrl(String input) {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(input);

        assertThrows(
                IllegalStateException.class,
                () ->
                        new AuditService(
                                mockSqs,
                                mockConfigurationService,
                                mockObjectMapper,
                                mockAuditEventFactory),
                "Null or empty queue url provided by configuration service");
    }
}
