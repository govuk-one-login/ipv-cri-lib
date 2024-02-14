package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventFactoryTest {

    private static final String TEST_AUDIT_EVENT_PREFIX = "AUDIT_EVENT_PREFIX";
    private static final String TEST_VC_ISSUER = "VC_ISSUER";

    @Mock private Clock mockClock;
    @Mock private ConfigurationService mockConfigurationService;

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowExceptionWhenAuditEventPrefixNullOrEmpty(String auditEventPrefix) {
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(auditEventPrefix);
        assertThrows(
                IllegalStateException.class,
                () -> new AuditEventFactory(mockConfigurationService, mockClock),
                "Audit event prefix not retrieved from configuration service");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowExceptionWhenVcIssuerNullOrEmpty(String vcIssuer) {
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(TEST_AUDIT_EVENT_PREFIX);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(vcIssuer);
        assertThrows(
                IllegalStateException.class,
                () -> new AuditEventFactory(mockConfigurationService, mockClock),
                "Issuer not retrieved from configuration service");
    }

    @Test
    void shouldThrowExceptionWhenClockIsNull() {
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(TEST_AUDIT_EVENT_PREFIX);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_VC_ISSUER);
        assertThrows(
                NullPointerException.class,
                () -> new AuditEventFactory(mockConfigurationService, null),
                "clock must not be null");
    }

    @Test
    void shouldCreateAuditEventWithContextAndExtensions() {
        long timestamp = 1656947224L;
        long eventTimestampMs = 165694722422L;
        String userId = String.valueOf(UUID.randomUUID());
        UUID sessionId = UUID.randomUUID();
        String persistentSessionId = UUID.randomUUID().toString();
        String clientSessionId = UUID.randomUUID().toString();
        String clientIpAddress = "81.145.61.43";
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(TEST_AUDIT_EVENT_PREFIX);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_VC_ISSUER);
        Instant mockInstant = mock(Instant.class);
        when(mockInstant.getEpochSecond()).thenReturn(timestamp);
        when(mockInstant.toEpochMilli()).thenReturn(eventTimestampMs);
        when(mockClock.instant()).thenReturn(mockInstant);
        PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);
        Map<String, String> requestHeaders = Map.of("X-Forwarded-For", clientIpAddress);
        SessionItem sessionItem = mock(SessionItem.class);
        when(sessionItem.getSubject()).thenReturn(userId);
        when(sessionItem.getSessionId()).thenReturn(sessionId);
        when(sessionItem.getPersistentSessionId()).thenReturn(persistentSessionId);
        when(sessionItem.getClientSessionId()).thenReturn(clientSessionId);
        AuditEventContext auditEventContext =
                new AuditEventContext(personIdentity, requestHeaders, sessionItem);
        AuditEventFactory auditEventFactory =
                new AuditEventFactory(mockConfigurationService, mockClock);
        Map<String, String> auditEventExtensions = Map.of("test", "extension-data");

        AuditEvent<Object> auditEvent =
                auditEventFactory.create("EVENT_TYPE", auditEventContext, auditEventExtensions);

        assertEquals(TEST_AUDIT_EVENT_PREFIX + "_EVENT_TYPE", auditEvent.getEvent());
        assertEquals(personIdentity, auditEvent.getRestricted());
        assertEquals(timestamp, auditEvent.getTimestamp());
        assertEquals(eventTimestampMs, auditEvent.getEventTimestampMs());
        assertEquals(userId, auditEvent.getUser().getUserId());
        assertEquals(clientIpAddress, auditEvent.getUser().getIpAddress());
        assertEquals(String.valueOf(sessionId), auditEvent.getUser().getSessionId());
        assertEquals(persistentSessionId, auditEvent.getUser().getPersistentSessionId());
        assertEquals(clientSessionId, auditEvent.getUser().getClientSessionId());

        assertEquals(auditEventExtensions, auditEvent.getExtensions());

        verify(mockClock, times(2)).instant();
        verify(mockInstant).toEpochMilli();
        verify(mockConfigurationService).getSqsAuditEventPrefix();
        verify(mockConfigurationService).getVerifiableCredentialIssuer();
    }

    @Test
    void shouldCreateAuditEventWithContext() {
        long timestamp = 1656947224L;
        long eventTimestampMs = 165694722422L;
        String clientIpAddress = "81.145.61.43";
        when(mockConfigurationService.getSqsAuditEventPrefix()).thenReturn(TEST_AUDIT_EVENT_PREFIX);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_VC_ISSUER);
        Instant mockInstant = mock(Instant.class);
        when(mockInstant.getEpochSecond()).thenReturn(timestamp);
        when(mockInstant.toEpochMilli()).thenReturn(eventTimestampMs);
        when(mockClock.instant()).thenReturn(mockInstant);
        PersonIdentityDetailed personIdentity = mock(PersonIdentityDetailed.class);
        Map<String, String> requestHeaders = Map.of("X-Forwarded-For", clientIpAddress);
        AuditEventContext auditEventContext =
                new AuditEventContext(personIdentity, requestHeaders, null);
        AuditEventFactory auditEventFactory =
                new AuditEventFactory(mockConfigurationService, mockClock);

        AuditEvent<Object> auditEvent =
                auditEventFactory.create("EVENT_TYPE", auditEventContext, null);

        assertEquals(TEST_AUDIT_EVENT_PREFIX + "_EVENT_TYPE", auditEvent.getEvent());
        assertEquals(personIdentity, auditEvent.getRestricted());
        assertEquals(timestamp, auditEvent.getTimestamp());
        assertEquals(eventTimestampMs, auditEvent.getEventTimestampMs());
        assertEquals(clientIpAddress, auditEvent.getUser().getIpAddress());
        assertNull(auditEvent.getUser().getUserId());
        assertNull(auditEvent.getUser().getSessionId());
        assertNull(auditEvent.getUser().getPersistentSessionId());
        assertNull(auditEvent.getUser().getClientSessionId());
        assertNull(auditEvent.getExtensions());
    }
}
