package uk.gov.di.ipv.cri.common.library.domain;

public enum AuditEventType {
    START, // Before a session is written to the Session table
    REQUEST_RECEIVED, // A non-common request has been received
    REQUEST_SENT, // When a third party call is started
    VC_ISSUED, // When the final VC is created in the issue credential lambda
    THIRD_PARTY_REQUEST_ENDED, // When a third party requests are ended
    RESPONSE_RECEIVED, // This is to replace THIRD_PARTY_REQUEST_ENDED
    END, // When VC credentials are being returned - final event

    // Passport-related
    IPV_PASSPORT_CRI_START, // Before a passport session is written to the Session table
    IPV_PASSPORT_CRI_REQUEST_SENT, // When a passport request call is started
    IPV_PASSPORT_CRI_VC_ISSUED, // When the final passport VC is issued
    IPV_PASSPORT_CRI_RESPONSE_RECEIVED, // When a third party passport request is ended
    IPV_PASSPORT_CRI_END // When VC credentials are being returned - final passport event
}
