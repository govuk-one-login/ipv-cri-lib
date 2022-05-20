package uk.gov.di.ipv.cri.address.library.domain.sharedclaims;

import java.time.LocalDate;
import java.util.List;

public class Name {
    private List<NamePart> nameParts;
    private LocalDate validFrom;
    private LocalDate validUntil;

    public List<NamePart> getNameParts() {
        return nameParts;
    }

    public void setNameParts(List<NamePart> nameParts) {
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
