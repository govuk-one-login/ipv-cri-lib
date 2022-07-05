package uk.gov.di.ipv.cri.common.library.domain;

import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;

import java.util.Map;

public class AuditEventContext {
    private final PersonIdentityDetailed personIdentity;
    private final Map<String, String> requestHeaders;
    private final SessionItem sessionItem;

    public AuditEventContext(
            PersonIdentityDetailed personIdentity,
            Map<String, String> requestHeaders,
            SessionItem sessionItem) {
        this.personIdentity = personIdentity;
        this.requestHeaders = requestHeaders;
        this.sessionItem = sessionItem;
    }

    public AuditEventContext(Map<String, String> requestHeaders, SessionItem sessionItem) {
        this(null, requestHeaders, sessionItem);
    }

    public PersonIdentityDetailed getPersonIdentity() {
        return personIdentity;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public SessionItem getSessionItem() {
        return sessionItem;
    }
}
