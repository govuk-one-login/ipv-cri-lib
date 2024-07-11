package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;

public class AuditService {
    private final SqsClient sqs;
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    private final AuditEventFactory auditEventFactory;

    public AuditService(
            SqsClient sqs,
            ConfigurationService configurationService,
            ObjectMapper objectMapper,
            AuditEventFactory auditEventFactory) {
        this.sqs = sqs;
        this.objectMapper = objectMapper.registerModule(new JavaTimeModule());
        this.auditEventFactory = auditEventFactory;
        this.queueUrl = configurationService.getSqsAuditEventQueueUrl();
        requireNonBlankQueueUrl();
    }

    private void requireNonBlankQueueUrl() {
        if (StringUtils.isBlank(this.queueUrl)) {
            throw new IllegalStateException(
                    "Null or empty queue url provided by configuration service");
        }
    }

    public void sendAuditEvent(AuditEventType eventType) throws SqsException {
        sendAuditEvent(eventType.toString(), null, null);
    }

    public void sendAuditEvent(String eventType) throws SqsException {
        sendAuditEvent(eventType, null, null);
    }

    public void sendAuditEvent(AuditEventType eventType, AuditEventContext context)
            throws SqsException {
        sendAuditEvent(eventType.toString(), context, null);
    }

    public void sendAuditEvent(String eventType, AuditEventContext context) throws SqsException {
        AuditEvent<Object> auditEvent = auditEventFactory.create(eventType, context, null);
        sendAuditEvent(auditEvent);
    }

    public <T> void sendAuditEvent(
            AuditEventType eventType, AuditEventContext context, T extensions) throws SqsException {
        sendAuditEvent(eventType.toString(), context, extensions);
    }

    public <T> void sendAuditEvent(String eventType, AuditEventContext context, T extensions)
            throws SqsException {
        AuditEvent<T> audiEvent = auditEventFactory.create(eventType, context, extensions);
        sendAuditEvent(audiEvent);
    }

    public <T> void sendAuditEvent(AuditEventType eventType, T extensions) throws SqsException {
        sendAuditEvent(eventType.toString(), extensions);
    }

    public <T> void sendAuditEvent(String eventType, T extensions) throws SqsException {
        AuditEvent<T> audiEvent = auditEventFactory.create(eventType, null, extensions);
        sendAuditEvent(audiEvent);
    }

    private <T> void sendAuditEvent(AuditEvent<T> auditEvent) throws SqsException {
        try {
            String serialisedAuditEvent = objectMapper.writeValueAsString(auditEvent);
            SendMessageRequest sendMessageRequest =
                    SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(serialisedAuditEvent)
                            .build();
            sqs.sendMessage(sendMessageRequest);
        } catch (JsonProcessingException e) {
            throw new SqsException(e);
        }
    }
}
