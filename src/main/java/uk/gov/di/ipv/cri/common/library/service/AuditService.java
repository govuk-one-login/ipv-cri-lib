package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;

import java.time.Instant;

public class AuditService {
    private final SqsClient sqs;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    private final String eventPrefix;

    public AuditService(
            SqsClient sqs, ConfigurationService configurationService, ObjectMapper objectMapper) {
        this.sqs = sqs;
        this.queueUrl = configurationService.getSqsAuditEventQueueUrl();
        this.objectMapper = objectMapper;
        this.eventPrefix = configurationService.getSqsAuditEventPrefix();
        if (eventPrefix == null || eventPrefix.isEmpty()) {
            throw new IllegalArgumentException("SQS event prefix is not set");
        }
    }

    public AuditService() {
        this(SqsClient.builder().build(), new ConfigurationService(), new ObjectMapper());
    }

    public void sendAuditEvent(AuditEventType eventType) throws SqsException {
        sendAuditEvent(eventType.toString());
    }

    public void sendAuditEvent(String eventType) throws SqsException {
        try {
            SendMessageRequest sendMessageRequest =
                    SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(generateMessageBody(eventType))
                            .build();
            sqs.sendMessage(sendMessageRequest);
        } catch (JsonProcessingException e) {
            throw new SqsException(e);
        }
    }

    private String generateMessageBody(String eventType) throws JsonProcessingException {
        String fullEventType =
                (eventPrefix == null || eventPrefix.isBlank())
                        ? eventType
                        : eventPrefix + "_" + eventType;
        AuditEvent auditEvent = new AuditEvent(Instant.now().getEpochSecond(), fullEventType);
        return objectMapper.writeValueAsString(auditEvent);
    }
}
