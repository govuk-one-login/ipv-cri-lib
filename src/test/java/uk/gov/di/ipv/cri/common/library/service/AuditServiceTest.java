package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    public String SQS_QUEUE_URL = "https://example-queue-url";

    @Mock SqsClient mockSqs;
    @Mock ConfigurationService mockConfigurationService;
    @Mock ObjectMapper mockObjectMapper;

    private AuditService auditService;
    private static Date fixedInstant;

    @BeforeAll
    static void beforeAll() {
        fixedInstant = new Date();
    }

    @BeforeEach
    void setup() {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(SQS_QUEUE_URL);
        auditService = new AuditService(mockSqs, mockConfigurationService, mockObjectMapper);
    }

    @Test
    void shouldSendMessageToSqsQueue() throws JsonProcessingException, SqsException {
        ArgumentCaptor<SendMessageRequest> sqsSendMessageRequestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        AuditEvent auditEvent = new AuditEvent(fixedInstant, AuditEventTypes.IPV_ADDRESS_CRI_START);
        String messageAuditEvent = new ObjectMapper().writeValueAsString(auditEvent);

        when(mockObjectMapper.writeValueAsString(any(AuditEvent.class)))
                .thenReturn(messageAuditEvent);

        SendMessageResponse mockSendMessageResponse = mock(SendMessageResponse.class);
        when(mockSqs.sendMessage(sqsSendMessageRequestCaptor.capture()))
                .thenReturn(mockSendMessageResponse);

        auditService.sendAuditEvent(AuditEventTypes.IPV_ADDRESS_CRI_START);
        SendMessageRequest capturedValue = sqsSendMessageRequestCaptor.getValue();
        verify(mockSqs).sendMessage(capturedValue);

        assertEquals(messageAuditEvent, capturedValue.messageBody());
        assertEquals(SQS_QUEUE_URL, capturedValue.queueUrl());
    }
}
