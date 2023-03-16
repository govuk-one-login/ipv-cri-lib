package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonIdentityDetailed {
    @JsonProperty("name")
    private final List<Name> names;

    @JsonProperty("birthDate")
    private final List<BirthDate> birthDates;

    @JsonProperty("address")
    private final List<Address> addresses;

    @JsonProperty("drivingPermit")
    private final List<DrivingPermit> drivingPermits;

    public PersonIdentityDetailed(
            List<Name> names, List<BirthDate> birthDates, List<Address> addresses) {
        this(names, birthDates, addresses, null);
    }

    public PersonIdentityDetailed(
            List<Name> names,
            List<BirthDate> birthDates,
            List<Address> addresses,
            List<DrivingPermit> drivingPermits) {
        this.names = names;
        this.birthDates = birthDates;
        this.addresses = addresses;
        this.drivingPermits = drivingPermits;
    }

    public List<Name> getNames() {
        return names;
    }

    public List<BirthDate> getBirthDates() {
        return birthDates;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public List<DrivingPermit> getDrivingPermits() {
        return drivingPermits;
    }
}
