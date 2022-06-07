package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import java.util.List;

public class Name {
    private List<NamePart> nameParts;

    public List<NamePart> getNameParts() {
        return nameParts;
    }

    public void setNameParts(List<NamePart> nameParts) {
        this.nameParts = nameParts;
    }
}
