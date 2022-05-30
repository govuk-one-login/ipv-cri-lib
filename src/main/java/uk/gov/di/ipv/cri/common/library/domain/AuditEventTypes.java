package uk.gov.di.ipv.cri.common.library.domain;

public enum AuditEventTypes {
    IPV_ADDRESS_CRI_START, // Before a session is written to the Session table for Address CRI
    IPV_ADDRESS_CRI_REQUEST_SENT, // When an address is added in the PUT /address API call
    IPV_ADDRESS_CRI_VC_ISSUED, // When the final VC is created in the issue credential lambda

    IPV_KBV_CRI_START, // Before a session is written to the Session table for KBV CRI
    IPV_KBV_CRI_REQUEST_SENT, // First request sent for the KBV Experian SOAP API
    IPV_KBV_CRI_EXPERIAN_REQUEST_ENDED, // First request sent for the KBV Experian SOAP API

    IPV_FRAUD_CRI_START // Before a session is written to the Session table for Fraud CRI
}
