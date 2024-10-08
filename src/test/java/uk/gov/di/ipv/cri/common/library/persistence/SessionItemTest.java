package uk.gov.di.ipv.cri.common.library.persistence;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionItemTest {
    private static final String INT_USER_CONTEXT = "international_user";

    @Mock private DataStore<SessionItem> dataStore;

    @InjectMocks private SessionService sessionService;

    private SessionItem sessionItem;

    @BeforeEach
    void setup() {
        sessionItem = new SessionItem();
        sessionItem.setSessionId(UUID.randomUUID());

        EvidenceRequest evidenceRequest = new EvidenceRequest();
        evidenceRequest.setScoringPolicy("gpg45");
        evidenceRequest.setStrengthScore(2);
        evidenceRequest.setValidityScore(3);
        evidenceRequest.setVerificationScore(4);
        evidenceRequest.setActivityHistoryScore(5);
        evidenceRequest.setIdentityFraudScore(6);
        sessionItem.setEvidenceRequest(evidenceRequest);

        sessionItem.setContext(INT_USER_CONTEXT);

        when(dataStore.getItem(any())).thenReturn(sessionItem);
    }

    @Test
    void testGetSessionItem() {
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
}
