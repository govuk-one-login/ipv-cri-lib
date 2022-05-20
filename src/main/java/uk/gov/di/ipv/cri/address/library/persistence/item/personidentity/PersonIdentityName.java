package uk.gov.di.ipv.cri.address.library.persistence.item.personidentity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;
import java.util.List;

@DynamoDbBean
public class PersonIdentityName {
    private List<PersonIdentityNamePart> nameParts;
    private LocalDate validFrom;
    private LocalDate validUntil;

    public List<PersonIdentityNamePart> getNameParts() {
        return nameParts;
    }

    public void setNameParts(List<PersonIdentityNamePart> nameParts) {
        this.nameParts = nameParts;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }
}
