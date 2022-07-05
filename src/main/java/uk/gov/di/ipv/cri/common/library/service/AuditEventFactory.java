package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import uk.gov.di.ipv.cri.common.library.domain.AuditEvent;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventContext;
import uk.gov.di.ipv.cri.common.library.domain.AuditEventUser;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;

public class AuditEventFactory {
    private static final String CLIENT_IP_HEADER_KEY = "X-Forwarded-For";

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
                        clock.instant().getEpochSecond(), eventPrefix + "_" + eventType, issuer);
        if (Objects.nonNull(auditEventContext)) {
            if (Objects.nonNull(auditEventContext.getPersonIdentity())) {
                auditEvent.setRestricted(auditEventContext.getPersonIdentity());
            }
            auditEvent.setUser(
                    createAuditEventUser(
                            auditEventContext.getRequestHeaders(),
                            auditEventContext.getSessionItem()));
        }
        if (Objects.nonNull(extensions)) {
            auditEvent.setExtensions(extensions);
        }
        return auditEvent;
    }

    private AuditEventUser createAuditEventUser(
            Map<String, String> requestHeaders, SessionItem sessionItem) {
        AuditEventUser userInfo = null;

        if (Objects.nonNull(sessionItem)) {
            userInfo = new AuditEventUser();
            userInfo.setSessionId(String.valueOf(sessionItem.getSessionId()));
            userInfo.setUserId(sessionItem.getSubject());
        }

        if (requestHeaders.containsKey(CLIENT_IP_HEADER_KEY)) {
            (Objects.nonNull(userInfo) ? userInfo : (userInfo = new AuditEventUser()))
                    .setIpAddress(requestHeaders.get(CLIENT_IP_HEADER_KEY));
        }

        return userInfo;
    }
}
