package uk.gov.di.ipv.cri.common.library.service;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.SharedClaims;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityItem;

import java.util.Optional;
import java.util.UUID;

public class PersonIdentityService {
    private static final String PERSON_IDENTITY_TABLE_NAME = "person-identity-common-cri-api";
    private final PersonIdentityMapper personIdentityMapper;
    private final ConfigurationService configurationService;
    private final DataStore<PersonIdentityItem> personIdentityDataStore;

    @ExcludeFromGeneratedCoverageReport
    public PersonIdentityService(
            ConfigurationService configurationService,
            DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this(
                new PersonIdentityMapper(),
                configurationService,
                new DataStore<>(
                        Optional.ofNullable(System.getenv("PERSON_IDENTITY_TABLE"))
                                .orElse(PERSON_IDENTITY_TABLE_NAME),
                        PersonIdentityItem.class,
                        dynamoDbEnhancedClient));
    }

    public PersonIdentityService(
            PersonIdentityMapper personIdentityMapper,
            ConfigurationService configurationService,
            DataStore<PersonIdentityItem> personIdentityDataStore) {
        this.personIdentityMapper = personIdentityMapper;
        this.configurationService = configurationService;
        this.personIdentityDataStore = personIdentityDataStore;
    }

    public void savePersonIdentity(UUID sessionId, SharedClaims sharedClaims) {
        PersonIdentityItem personIdentityItem =
                personIdentityMapper.mapToPersonIdentityItem(sharedClaims);
        personIdentityItem.setSessionId(sessionId);
        personIdentityItem.setExpiryDate(configurationService.getSessionExpirationEpoch());

        this.personIdentityDataStore.create(personIdentityItem);
    }

    public PersonIdentity getPersonIdentity(UUID sessionId) {
        PersonIdentityItem personIdentityItem = getById(sessionId);
        return personIdentityMapper.mapToPersonIdentity(personIdentityItem);
    }

    public PersonIdentityDetailed getPersonIdentityDetailed(UUID sessionId) {
        PersonIdentityItem personIdentityItem = getById(sessionId);
        return personIdentityMapper.mapToPersonIdentityDetailed(personIdentityItem);
    }

    public PersonIdentity convertToPersonIdentitySummary(
            PersonIdentityDetailed personIdentityDetailed) {
        return personIdentityMapper.mapToPersonIdentity(personIdentityDetailed);
    }

    private PersonIdentityItem getById(UUID sessionId) {
        return this.personIdentityDataStore.getItem(String.valueOf(sessionId));
    }
}
