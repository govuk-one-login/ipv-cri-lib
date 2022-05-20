package uk.gov.di.ipv.cri.common.library.persistence.item.personidentity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;

@DynamoDbBean
public class PersonIdentityDateOfBirth {
    private LocalDate value;

    public LocalDate getValue() {
        return value;
    }

    public void setValue(LocalDate value) {
        this.value = value;
    }
}
