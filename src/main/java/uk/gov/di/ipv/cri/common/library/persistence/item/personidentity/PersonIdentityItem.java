package uk.gov.di.ipv.cri.common.library.persistence.item.personidentity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import uk.gov.di.ipv.cri.common.library.domain.CanonicalAddress;

import java.util.List;
import java.util.UUID;

@DynamoDbBean
public class PersonIdentityItem {
    private UUID sessionId;
    private List<CanonicalAddress> addresses;
    private List<PersonIdentityName> names;
    private List<PersonIdentityDateOfBirth> birthDates;
    private long expiryDate;

    @DynamoDbPartitionKey()
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public List<CanonicalAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<CanonicalAddress> addresses) {
        this.addresses = addresses;
    }

    public List<PersonIdentityName> getNames() {
        return names;
    }

    public void setNames(List<PersonIdentityName> names) {
        this.names = names;
    }

    public List<PersonIdentityDateOfBirth> getBirthDates() {
        return birthDates;
    }

    public void setBirthDates(List<PersonIdentityDateOfBirth> dateOfBirth) {
        this.birthDates = dateOfBirth;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }
}
