package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;

import java.time.Clock;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    private final String SQS_QUEUE_URL = "https://example-queue-url";

    private final String SQS_PREFIX = "TEST";

    @Mock private SqsClient mockSqs;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private ObjectMapper mockObjectMapper;

    @Mock private Clock mockClock;
    private AuditService auditService;

    @Test
    void shouldSendMessageToSqsQueue() throws JsonProcessingException, SqsException {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(SQS_QUEUE_URL);
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(SQS_PREFIX);
        Instant fixedInstant = Instant.now();
        when(mockClock.instant()).thenReturn(fixedInstant);

        auditService =
                new AuditService(mockSqs, mockConfigurationService, mockObjectMapper, mockClock);

        ArgumentCaptor<SendMessageRequest> sqsSendMessageRequestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        AuditEvent auditEvent =
                new AuditEvent(
                        fixedInstant.getEpochSecond(),
                        AuditEventType.START.toString(),
                        "https://cri-issuer");
        String messageAuditEvent = new ObjectMapper().writeValueAsString(auditEvent);

        when(mockObjectMapper.writeValueAsString(any(AuditEvent.class)))
                .thenReturn(messageAuditEvent);

        SendMessageResponse mockSendMessageResponse = mock(SendMessageResponse.class);
        when(mockSqs.sendMessage(sqsSendMessageRequestCaptor.capture()))
                .thenReturn(mockSendMessageResponse);

        auditService.sendAuditEvent(AuditEventType.START);
        SendMessageRequest capturedValue = sqsSendMessageRequestCaptor.getValue();
        verify(mockSqs).sendMessage(capturedValue);

        assertEquals(messageAuditEvent, capturedValue.messageBody());
        assertEquals(SQS_QUEUE_URL, capturedValue.queueUrl());
    }

    @Test
    void shouldCorrectlyAddPrefix() throws SqsException, JsonProcessingException {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(SQS_QUEUE_URL);
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(SQS_PREFIX);
        Instant fixedInstant = Instant.now();
        when(mockClock.instant()).thenReturn(fixedInstant);

        auditService =
                new AuditService(mockSqs, mockConfigurationService, mockObjectMapper, mockClock);

        ArgumentCaptor<SendMessageRequest> sqsSendMessageRequestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        AuditEvent auditEvent =
                new AuditEvent(
                        fixedInstant.getEpochSecond(),
                        SQS_PREFIX + "_" + AuditEventType.START,
                        "https://cri-issuer");
        String messageAuditEvent = new ObjectMapper().writeValueAsString(auditEvent);
        when(mockObjectMapper.writeValueAsString(any(AuditEvent.class)))
                .thenReturn(messageAuditEvent);

        SendMessageResponse mockSendMessageResponse = mock(SendMessageResponse.class);
        when(mockSqs.sendMessage(sqsSendMessageRequestCaptor.capture()))
                .thenReturn(mockSendMessageResponse);

        auditService.sendAuditEvent(AuditEventType.START);
        SendMessageRequest capturedValue = sqsSendMessageRequestCaptor.getValue();
        verify(mockSqs).sendMessage(capturedValue);

        assertThat(
                capturedValue.messageBody(),
                containsString(SQS_PREFIX + "_" + AuditEventType.START));
        assertEquals(SQS_QUEUE_URL, capturedValue.queueUrl());
    }

    @Test
    void ConstructorShouldThrowErrorWhenNoPrefix() throws SqsException {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(SQS_QUEUE_URL);
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn("");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AuditService(
                                mockSqs, mockConfigurationService, mockObjectMapper, mockClock));
    }
}
