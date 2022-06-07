package uk.gov.di.ipv.cri.common.library.persistence.item.personidentity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@DynamoDbBean
public class PersonIdentityName {
    private List<PersonIdentityNamePart> nameParts;

    public List<PersonIdentityNamePart> getNameParts() {
        return nameParts;
    }

    public void setNameParts(List<PersonIdentityNamePart> nameParts) {
        this.nameParts = nameParts;
    }
}
