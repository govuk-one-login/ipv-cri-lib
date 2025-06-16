package uk.gov.di.ipv.cri.common.library.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.SessionService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionItemTest {
    private static final String INT_USER_CONTEXT = "international_user";

    @Mock private DataStore<SessionItem> dataStore;

    @InjectMocks private SessionService sessionService;

    private SessionItem createSessionItem(
            String scoringPolicy,
            Integer strengthScore,
            Integer validityScore,
            Integer verificationScore,
            Integer activityHistoryScore,
            Integer identityFraudScore) {
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(UUID.randomUUID());

        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setScoringPolicy(scoringPolicy);
        evidenceRequest.setStrengthScore(strengthScore);
        evidenceRequest.setValidityScore(validityScore);
        evidenceRequest.setVerificationScore(verificationScore);
        evidenceRequest.setActivityHistoryScore(activityHistoryScore);
        evidenceRequest.setIdentityFraudScore(identityFraudScore);
        sessionItem.setEvidenceRequest(evidenceRequest);

        sessionItem.setContext(INT_USER_CONTEXT);

        return sessionItem;
    }

    @Test
    void testGetValidSessionItem() {
        SessionItem sessionItem = createSessionItem("gpg45", 2, 3, 4, 5, 6);
        when(dataStore.getItem(any())).thenReturn(sessionItem);
        SessionItem retrievedItem =
                sessionService.getSession(sessionItem.getSessionId().toString());

        EvidenceRequest evidenceRequest = sessionItem.getEvidenceRequest();

        assertEquals(sessionItem.getSessionId(), retrievedItem.getSessionId());
        assertEquals("gpg45", evidenceRequest.getScoringPolicy());
        assertEquals(2, evidenceRequest.getStrengthScore());
        assertEquals(3, evidenceRequest.getValidityScore());
        assertEquals(4, evidenceRequest.getVerificationScore());
        assertEquals(5, evidenceRequest.getActivityHistoryScore());
        assertEquals(6, evidenceRequest.getIdentityFraudScore());

        assertEquals(INT_USER_CONTEXT, sessionItem.getContext());
    }

    @Test
    void testGetSessionItemWhenEvidenceRequestIsNull() {
        SessionItem sessionItem = createSessionItem(null, null, null, null, null, null);
        when(dataStore.getItem(any())).thenReturn(sessionItem);
        SessionItem retrievedItem =
                sessionService.getSession(sessionItem.getSessionId().toString());

        EvidenceRequest evidenceRequest = sessionItem.getEvidenceRequest();

        assertEquals(sessionItem.getSessionId(), retrievedItem.getSessionId());
        assertEquals(sessionItem.getContext(), retrievedItem.getContext());
        assertNull(evidenceRequest.getScoringPolicy());
        assertNull(evidenceRequest.getStrengthScore());
        assertNull(evidenceRequest.getValidityScore());
        assertNull(evidenceRequest.getVerificationScore());
        assertNull(evidenceRequest.getActivityHistoryScore());
        assertNull(evidenceRequest.getIdentityFraudScore());

        assertEquals(INT_USER_CONTEXT, sessionItem.getContext());
    }
}
