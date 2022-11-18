package uk.gov.di.ipv.cri.common.library.domain;

public enum AuditEventType {
    START, // Before a session is written to the Session table
    REQUEST_RECEIVED, // A non-common request has been received
    REQUEST_SENT, // When a third party call is started
    VC_ISSUED, // When the final VC is created in the issue credential lambda
    THIRD_PARTY_REQUEST_ENDED, // When a third party requests are ended
    END, // When VC credentials are being returned - final event
}
