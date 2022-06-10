package uk.gov.di.ipv.cri.common.library.domain;

public enum AuditEventType {
    START, // Before a session is written to the Session table
    REQUEST_RECEIVED, // A non-common request has been received
    REQUEST_SENT, // When a third party call is started
    VC_ISSUED, // When the final VC is created in the issue credential lambda
    THIRD_PARTY_REQUEST_ENDED, // When a third party requests are ended

    // Note: The following events are not used in the current implementation, but are here for
    // future use
    THIRD_PARTY_REQUEST_STARTED, // When third party requests are started
    THIRD_PARTY_REQUEST_SENT, // When a third party call is actually sent
    ERROR_RAISED, // When an error is raised in the lambda
}
