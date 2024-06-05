package uk.gov.di.ipv.cri.common.library.persistence.item.personidentity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class PersonIdentitySocialSecurityRecord {
    private String personalNumber;

    public String getPersonalNumber() {
        return personalNumber;
    }

    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }
}
