package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SharedClaims {
    @JsonProperty("socialSecurityRecord")
    private List<SocialSecurityRecord> socialSecurityRecords;

    @JsonProperty("name")
    private List<Name> names;

    @JsonProperty("birthDate")
    private List<BirthDate> birthDates;

    @JsonProperty("@context")
    private List<String> context;

    @JsonProperty("address")
    private List<Address> addresses;

    @JsonProperty("drivingPermit")
    private List<DrivingPermit> drivingPermits;

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

    public List<SocialSecurityRecord> getSocialSecurityRecords() {
        return socialSecurityRecords;
    }

    public void setSocialSecurityRecords(List<SocialSecurityRecord> socialSecurityRecords) {
        this.socialSecurityRecords = socialSecurityRecords;
    }

    public List<DrivingPermit> getDrivingPermits() {
        return drivingPermits;
    }

    public void setDrivingPermits(List<DrivingPermit> drivingPermits) {
        this.drivingPermits = drivingPermits;
    }
}
