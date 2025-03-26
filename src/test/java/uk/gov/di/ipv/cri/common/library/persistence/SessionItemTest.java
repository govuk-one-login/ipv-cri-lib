package uk.gov.di.ipv.cri.common.library.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import uk.gov.di.ipv.cri.common.library.persistence.item.EvidenceRequest;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.gov.di.ipv.cri.common.library.service.SessionService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionItemTest {
    private static final String INT_USER_CONTEXT = "international_user";

    private DataStore<SessionItem> dataStore;
    private SessionService sessionService;
    private SessionItem sessionItem;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        DynamoDbEnhancedClient dynamoDbEnhancedClient = mock(DynamoDbEnhancedClient.class);
        when(dynamoDbEnhancedClient.table(any(), any())).thenReturn(mock(DynamoDbTable.class));

        dataStore = new DataStore<>("dummy-table", SessionItem.class, dynamoDbEnhancedClient);
        sessionService =
                new SessionService(mock(ConfigurationService.class), dynamoDbEnhancedClient);
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
}
