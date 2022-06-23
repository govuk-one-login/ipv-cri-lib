package uk.gov.di.ipv.cri.common.library.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.exception.SqsException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        assertThat(capturedValue.messageBody(), containsString("component_id"));
        assertThat(capturedValue.messageBody(), containsString("https://cri-issuer"));
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

    @Test
    void shouldAddRestrictedData() throws SqsException {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(SQS_QUEUE_URL);
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(SQS_PREFIX);
        when(mockClock.instant()).thenReturn(Instant.now());

        auditService =
                new AuditService(
                        mockSqs,
                        mockConfigurationService,
                        new ObjectMapper().registerModule(new JavaTimeModule()),
                        mockClock);

        PersonIdentityDetailed personIdentity = createPersonIdentity();

        ArgumentCaptor<SendMessageRequest> sqsSendMessageRequestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        SendMessageResponse mockSendMessageResponse = mock(SendMessageResponse.class);
        when(mockSqs.sendMessage(sqsSendMessageRequestCaptor.capture()))
                .thenReturn(mockSendMessageResponse);

        auditService.sendAuditEvent(AuditEventType.START, personIdentity);
        SendMessageRequest capturedValue = sqsSendMessageRequestCaptor.getValue();
        verify(mockSqs).sendMessage(capturedValue);

        assertThat(capturedValue.messageBody(), containsString("Joe"));
        assertEquals(SQS_QUEUE_URL, capturedValue.queueUrl());
    }

    @Test
    void shouldAddExtensionsMap() throws SqsException {
        when(mockConfigurationService.getSqsAuditEventQueueUrl()).thenReturn(SQS_QUEUE_URL);
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(SQS_PREFIX);
        when(mockClock.instant()).thenReturn(Instant.now());

        auditService =
                new AuditService(mockSqs, mockConfigurationService, new ObjectMapper(), mockClock);

        ArgumentCaptor<SendMessageRequest> sqsSendMessageRequestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);
        SendMessageResponse mockSendMessageResponse = mock(SendMessageResponse.class);
        when(mockSqs.sendMessage(sqsSendMessageRequestCaptor.capture()))
                .thenReturn(mockSendMessageResponse);

        auditService.sendAuditEvent(AuditEventType.START, Map.of("foo", "bar"));
        SendMessageRequest capturedValue = sqsSendMessageRequestCaptor.getValue();
        verify(mockSqs).sendMessage(capturedValue);

        assertThat(capturedValue.messageBody(), containsString("foo"));
        assertEquals(SQS_QUEUE_URL, capturedValue.queueUrl());
    }

    private PersonIdentityDetailed createPersonIdentity() {
        Address address = new Address();
        address.setBuildingNumber("114");
        address.setStreetName("Wellington Street");
        address.setPostalCode("LS1 1BA");

        Name name = new Name();
        NamePart firstNamePart = new NamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Joe");
        NamePart surnamePart = new NamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Bloggs");
        name.setNameParts(List.of(firstNamePart, surnamePart));

        BirthDate birthDate = new BirthDate();
        birthDate.setValue(LocalDate.of(1980, 1, 1));

        return new PersonIdentityDetailed(List.of(name), List.of(birthDate), List.of(address));
    }
}
