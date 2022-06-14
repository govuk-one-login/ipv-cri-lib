package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.SharedClaims;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityItem;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonIdentityServiceTest {
    private static final UUID TEST_SESSION_ID = UUID.randomUUID();
    @Mock private PersonIdentityMapper mockPersonIdentityMapper;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private DataStore<PersonIdentityItem> mockPersonIdentityDataStore;
    @InjectMocks private PersonIdentityService personIdentityService;

    @Test
    void shouldSavePersonIdentity() {
        final long sessionExpirationEpoch = 1655203777L;
        SharedClaims testSharedClaims = new SharedClaims();
        PersonIdentityItem testPersonIdentityItem = mock(PersonIdentityItem.class);

        when(mockPersonIdentityMapper.mapToPersonIdentityItem(testSharedClaims))
                .thenReturn(testPersonIdentityItem);
        when(mockConfigurationService.getSessionExpirationEpoch())
                .thenReturn(sessionExpirationEpoch);

        personIdentityService.savePersonIdentity(TEST_SESSION_ID, testSharedClaims);

        verify(mockPersonIdentityMapper).mapToPersonIdentityItem(testSharedClaims);
        verify(testPersonIdentityItem).setSessionId(TEST_SESSION_ID);
        verify(mockConfigurationService).getSessionExpirationEpoch();
        verify(testPersonIdentityItem).setExpiryDate(sessionExpirationEpoch);
        verify(mockPersonIdentityDataStore).create(testPersonIdentityItem);
    }

    @Test
    void shouldGetPersonIdentity() {
        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        PersonIdentity testPersonIdentity = new PersonIdentity();

        when(mockPersonIdentityDataStore.getItem(String.valueOf(TEST_SESSION_ID)))
                .thenReturn(testPersonIdentityItem);
        when(mockPersonIdentityMapper.mapToPersonIdentity(testPersonIdentityItem))
                .thenReturn(testPersonIdentity);

        PersonIdentity retrievedPersonIdentity =
                personIdentityService.getPersonIdentity(TEST_SESSION_ID);

        verify(mockPersonIdentityDataStore).getItem(String.valueOf(TEST_SESSION_ID));
        verify(mockPersonIdentityMapper).mapToPersonIdentity(testPersonIdentityItem);
        assertEquals(testPersonIdentity, retrievedPersonIdentity);
    }
}
