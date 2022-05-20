package uk.gov.di.ipv.cri.address.library.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.address.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.address.library.domain.sharedclaims.SharedClaims;
import uk.gov.di.ipv.cri.address.library.persistence.DataStore;
import uk.gov.di.ipv.cri.address.library.persistence.item.personidentity.PersonIdentityItem;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    @Mock private Clock mockClock;
    @InjectMocks private PersonIdentityService personIdentityService;

    @Test
    void shouldSavePersonIdentity() {
        long sessionTtl = 30L;
        SharedClaims testSharedClaims = new SharedClaims();
        PersonIdentityItem testPersonIdentityItem = mock(PersonIdentityItem.class);
        Instant instant = Instant.now();

        when(mockPersonIdentityMapper.mapToPersonIdentityItem(testSharedClaims))
                .thenReturn(testPersonIdentityItem);
        when(mockConfigurationService.getSessionTtl()).thenReturn(sessionTtl);
        when(mockClock.instant()).thenReturn(instant);

        personIdentityService.savePersonIdentity(TEST_SESSION_ID, testSharedClaims);

        verify(mockPersonIdentityMapper).mapToPersonIdentityItem(testSharedClaims);
        verify(testPersonIdentityItem).setSessionId(TEST_SESSION_ID);
        verify(mockConfigurationService).getSessionTtl();
        verify(testPersonIdentityItem)
                .setExpiryDate(instant.plus(sessionTtl, ChronoUnit.SECONDS).getEpochSecond());
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
