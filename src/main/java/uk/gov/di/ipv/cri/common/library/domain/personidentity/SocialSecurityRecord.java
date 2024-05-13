package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentitySocialSecurityRecord;

public class SocialSecurityRecord {

    private String personalNumber;

    public SocialSecurityRecord() {}

    public SocialSecurityRecord(PersonIdentitySocialSecurityRecord socialSecurityRecord) {
        this.personalNumber = socialSecurityRecord.getPersonalNumber();
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }
}
