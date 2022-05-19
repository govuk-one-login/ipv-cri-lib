package uk.gov.di.ipv.cri.common.library.service;

import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.SharedClaims;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.DynamoDbEnhancedClientFactory;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityItem;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class PersonIdentityService {
    private final PersonIdentityMapper personIdentityMapper;
    private final ConfigurationService configurationService;
    private final DataStore<PersonIdentityItem> personIdentityDataStore;
    private final Clock clock;

    @ExcludeFromGeneratedCoverageReport
    public PersonIdentityService() {
        this.configurationService = new ConfigurationService();
        this.personIdentityMapper = new PersonIdentityMapper();
        this.personIdentityDataStore =
                new DataStore<>(
                        configurationService.getPersonIdentityTableName(),
                        PersonIdentityItem.class,
                        new DynamoDbEnhancedClientFactory().getClient());
        this.clock = Clock.systemUTC();
    }

    public PersonIdentityService(
            PersonIdentityMapper personIdentityMapper,
            ConfigurationService configurationService,
            DataStore<PersonIdentityItem> personIdentityDataStore,
            Clock clock) {
        this.personIdentityMapper = personIdentityMapper;
        this.configurationService = configurationService;
        this.personIdentityDataStore = personIdentityDataStore;
        this.clock = clock;
    }

    public void savePersonIdentity(UUID sessionId, SharedClaims sharedClaims) {
        PersonIdentityItem personIdentityItem =
                personIdentityMapper.mapToPersonIdentityItem(sharedClaims);
        personIdentityItem.setSessionId(sessionId);
        personIdentityItem.setExpiryDate(
                clock.instant()
                        .plus(configurationService.getSessionTtl(), ChronoUnit.SECONDS)
                        .getEpochSecond());

        this.personIdentityDataStore.create(personIdentityItem);
    }

    public PersonIdentity getPersonIdentity(UUID sessionId) {
        PersonIdentityItem personIdentityItem =
                this.personIdentityDataStore.getItem(String.valueOf(sessionId));
        return personIdentityMapper.mapToPersonIdentity(personIdentityItem);
    }
}
