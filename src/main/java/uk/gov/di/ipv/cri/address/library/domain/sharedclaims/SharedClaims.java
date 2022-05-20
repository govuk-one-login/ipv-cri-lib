package uk.gov.di.ipv.cri.address.library.domain.sharedclaims;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SharedClaims {
    @JsonProperty("name")
    private List<Name> names;

    @JsonProperty("birthDate")
    private List<BirthDate> birthDates;

    @JsonProperty("@context")
    private List<String> context;

    @JsonProperty("address")
    private List<Address> addresses;

    public List<Name> getNames() {
        return names;
    }

    public void setNames(List<Name> names) {
        this.names = names;
    }

    public List<BirthDate> getBirthDates() {
        return birthDates;
    }

    public void setBirthDates(List<BirthDate> birthDates) {
        this.birthDates = birthDates;
    }

    public List<String> getContext() {
        return context;
    }

    public void setContext(List<String> context) {
        this.context = context;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }
}
