package uk.gov.di.ipv.cri.address.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.di.ipv.cri.address.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.address.library.domain.AuditEventTypes;
import uk.gov.di.ipv.cri.address.library.exception.SqsException;

import java.time.Instant;
import java.util.UUID;

public class AuditService {
    private final SqsClient sqs;
    private final String queueUrl;

    public AuditService(SqsClient sqs, ConfigurationService configurationService) {
        this.sqs = sqs;
        this.queueUrl = configurationService.getSqsAuditEventQueueUrl();
    }

    public void sendAuditEvent(AuditEventTypes eventType, UUID sessionId, String clientId)
            throws SqsException {
        try {
            SendMessageRequest sendMessageRequest =
                    SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(generateMessageBody(eventType, sessionId, clientId))
                            .build();
            sqs.sendMessage(sendMessageRequest);
        } catch (JsonProcessingException e) {
            throw new SqsException(e);
        }
    }

    private String generateMessageBody(AuditEventTypes eventType, UUID sessionId, String clientId)
            throws JsonProcessingException {
        AuditEvent auditEvent = new AuditEvent();
        Instant now = Instant.now();
        int instant = (int) now.getEpochSecond();
        auditEvent.setTimestamp(instant);
        auditEvent.setEvent(eventType);
        auditEvent.setClientId(clientId);
        auditEvent.setEventId(sessionId.toString());
        auditEvent.setTimestampFormatted(now.toString());
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(auditEvent);
    }
}
