package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;

import java.time.Clock;
import java.util.Objects;

public class AuditService {
    private final SqsClient sqs;
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    private final String eventPrefix;
    private final String issuer;
    private final Clock clock;

    @ExcludeFromGeneratedCoverageReport
    public AuditService() {
        this(
                SqsClient.builder().build(),
                new ConfigurationService(),
                new ObjectMapper(),
                Clock.systemUTC());
    }

    public AuditService(
            SqsClient sqs,
            ConfigurationService configurationService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.sqs = sqs;
        this.queueUrl = configurationService.getSqsAuditEventQueueUrl();
        this.objectMapper = objectMapper;
        this.eventPrefix = configurationService.getSqsAuditEventPrefix();
        this.issuer = configurationService.getVerifiableCredentialIssuer();
        if (StringUtils.isBlank(eventPrefix)) {
            throw new IllegalArgumentException("SQS event prefix is not set");
        }
        this.clock = clock;
    }

    public void sendAuditEvent(AuditEventType eventType) throws SqsException {
        String messageBody = generateMessageBody(eventType.toString(), null, null);
        sendAuditEventWithMessageBody(messageBody);
    }

    public void sendAuditEvent(String eventType) throws SqsException {
        String messageBody = generateMessageBody(eventType, null, null);
        sendAuditEventWithMessageBody(messageBody);
    }

    public void sendAuditEvent(AuditEventType eventType, PersonIdentityDetailed restricted)
            throws SqsException {
        String messageBody = generateMessageBody(eventType.toString(), restricted, null);
        sendAuditEventWithMessageBody(messageBody);
    }

    public void sendAuditEvent(String eventType, PersonIdentityDetailed restricted)
            throws SqsException {
        String messageBody = generateMessageBody(eventType, restricted, null);
        sendAuditEventWithMessageBody(messageBody);
    }

    public <T> void sendAuditEvent(AuditEventType eventType, T extensions) throws SqsException {
        String messageBody = generateMessageBody(eventType.toString(), null, extensions);
        sendAuditEventWithMessageBody(messageBody);
    }

    public <T> void sendAuditEvent(String eventType, T extensions) throws SqsException {
        String messageBody = generateMessageBody(eventType, null, extensions);
        sendAuditEventWithMessageBody(messageBody);
    }

    public <T> void sendAuditEvent(
            AuditEventType eventType, PersonIdentityDetailed restricted, T extensions)
            throws SqsException {
        String messageBody = generateMessageBody(eventType.toString(), restricted, extensions);
        sendAuditEventWithMessageBody(messageBody);
    }

    public <T> void sendAuditEvent(
            String eventType, PersonIdentityDetailed restricted, T extensions) throws SqsException {
        String messageBody = generateMessageBody(eventType, restricted, extensions);
        sendAuditEventWithMessageBody(messageBody);
    }

    private <T> String generateMessageBody(
            String eventType, PersonIdentityDetailed restricted, T extensions) throws SqsException {
        try {
            AuditEvent<T> auditEvent =
                    new AuditEvent<>(
                            clock.instant().getEpochSecond(),
                            eventPrefix + "_" + eventType,
                            issuer);
            if (Objects.nonNull(restricted)) {
                auditEvent.setRestricted(restricted);
            }
            if (Objects.nonNull(extensions)) {
                auditEvent.setExtensions(extensions);
            }
            return objectMapper.writeValueAsString(auditEvent);
        } catch (JsonProcessingException e) {
            throw new SqsException(e);
        }
    }

    private void sendAuditEventWithMessageBody(String messageBody) {
        SendMessageRequest sendMessageRequest =
                SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build();
        sqs.sendMessage(sendMessageRequest);
    }
}
