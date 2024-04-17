package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventUser;
import uk.gov.di.ipv.cri.common.library.domain.DeviceInformation;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;

public class AuditEventFactory {
    private static final String CLIENT_IP_HEADER_KEY = "X-Forwarded-For";
    private static final String TXMA_AUDIT_ENCODED = "txma-audit-encoded";

    private final String eventPrefix;
    private final String issuer;
    private final Clock clock;

    public AuditEventFactory(ConfigurationService configurationService, Clock clock) {
        this.eventPrefix = configurationService.getSqsAuditEventPrefix();
        if (StringUtils.isBlank(this.eventPrefix)) {
            throw new IllegalStateException(
                    "Audit event prefix not retrieved from configuration service");
        }
        this.issuer = configurationService.getVerifiableCredentialIssuer();
        if (StringUtils.isBlank(this.issuer)) {
            throw new IllegalStateException("Issuer not retrieved from configuration service");
        }
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    <T> AuditEvent<T> create(String eventType, AuditEventContext auditEventContext, T extensions) {
        AuditEvent<T> auditEvent =
                new AuditEvent<>(
                        clock.instant().getEpochSecond(),
                        clock.instant().toEpochMilli(),
                        eventPrefix + "_" + eventType,
                        issuer);
        if (Objects.nonNull(auditEventContext)) {
            Map<String, String> requestHeaders = auditEventContext.getRequestHeaders();

            auditEvent.setRestricted(
                    createAuditEventRestricted(
                            requestHeaders, auditEventContext.getPersonIdentity()));

            auditEvent.setUser(
                    createAuditEventUser(requestHeaders, auditEventContext.getSessionItem()));
        }
        if (Objects.nonNull(extensions)) {
            auditEvent.setExtensions(extensions);
        }
        return auditEvent;
    }

    @SuppressWarnings("deprecation")
    private PersonIdentityDetailed createAuditEventRestricted(
            Map<String, String> requestHeaders, PersonIdentityDetailed personIdentityDetails) {
        PersonIdentityDetailed restricted = personIdentityDetails;

        if (requestHeaders.containsKey(TXMA_AUDIT_ENCODED)) {
            if (Objects.isNull(restricted)) {
                restricted = new PersonIdentityDetailed(null, null, null, null, null);
            }
            DeviceInformation deviceInformation = new DeviceInformation();
            deviceInformation.setEncoded(requestHeaders.get(TXMA_AUDIT_ENCODED));
            restricted.setDeviceInformation(deviceInformation);
        }

        return restricted;
    }

    private AuditEventUser createAuditEventUser(
            Map<String, String> requestHeaders, SessionItem sessionItem) {
        AuditEventUser userInfo = null;

        if (Objects.nonNull(sessionItem)) {
            userInfo = new AuditEventUser();
            userInfo.setSessionId(String.valueOf(sessionItem.getSessionId()));
            userInfo.setUserId(sessionItem.getSubject());
            userInfo.setPersistentSessionId(sessionItem.getPersistentSessionId());
            userInfo.setClientSessionId(sessionItem.getClientSessionId());
        }

        if (requestHeaders.containsKey(CLIENT_IP_HEADER_KEY)) {
            if (Objects.isNull(userInfo)) {
                userInfo = new AuditEventUser();
            }
            userInfo.setIpAddress(requestHeaders.get(CLIENT_IP_HEADER_KEY));
        }

        return userInfo;
    }
}
