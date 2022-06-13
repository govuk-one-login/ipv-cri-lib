package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.utils.StringUtils;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventType;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;

import java.time.Clock;
import java.time.Instant;

public class AuditService {
    private final SqsClient sqs;
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    private final String eventPrefix;

    private final Clock clock;

    @ExcludeFromGeneratedCoverageReport
    public AuditService() {
        this(SqsClient.builder().build(), new ConfigurationService(), new ObjectMapper(), Clock.systemUTC());
    }

    public AuditService(
            SqsClient sqs, ConfigurationService configurationService, ObjectMapper objectMapper, Clock clock) {
        this.sqs = sqs;
        this.queueUrl = configurationService.getSqsAuditEventQueueUrl();
        this.objectMapper = objectMapper;
        this.eventPrefix = configurationService.getSqsAuditEventPrefix();
        if (StringUtils.isBlank(eventPrefix)) {
            throw new IllegalArgumentException("SQS event prefix is not set");
        }
        this.clock = clock;
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
        AuditEvent auditEvent = new AuditEvent(
                clock.instant().getEpochSecond(),
                eventPrefix + "_" + eventType
        );
        return objectMapper.writeValueAsString(auditEvent);
    }
}
