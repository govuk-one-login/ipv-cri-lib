package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.di.ipv.cri.common.library.domain.DeviceInformation;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonIdentityDetailed {

    @JsonProperty("name")
    private final List<Name> names;

    @JsonProperty("birthDate")
    private final List<BirthDate> birthDates;

    @JsonProperty("address")
    private final List<Address> addresses;

    @JsonProperty("drivingPermit")
    private final List<DrivingPermit> drivingPermits;

    @JsonProperty("passport")
    private final List<Passport> passports;

    @JsonProperty("socialSecurityRecord")
    private final List<SocialSecurityRecord> socialSecurityRecords;

    @JsonProperty("device_information")
    private DeviceInformation deviceInformation;

    /**
     * @param names list of names
     * @param birthDates list of birthDates
     * @deprecated Avoid use outside CRI-Lib, Restricted section of audits should be made generic in
     *     future and provided by the CRI To avoid the current pattern of adding CRI specific fields
     *     to this class
     * @see uk.gov.di.ipv.cri.common.library.service.PersonIdentityDetailedFactory
     *     createPersonIdentityDetailedWith methods
     */
    @Deprecated(since = "1.5.0")
    public PersonIdentityDetailed(List<Name> names, List<BirthDate> birthDates) {
        this(names, birthDates, null, null, null, null);
    }

    /**
     * @param names list of names
     * @param birthDates list of birthDates
     * @param addresses list of addresses, null list if not used
     * @deprecated Avoid use outside CRI-Lib.
     * @see uk.gov.di.ipv.cri.common.library.service.PersonIdentityDetailedFactory
     *     createPersonIdentityDetailedWith methods
     */
    @Deprecated(since = "1.5.0")
    public PersonIdentityDetailed(
            List<Name> names, List<BirthDate> birthDates, List<Address> addresses) {
        this(names, birthDates, addresses, null, null, null);
    }

    /**
     * @param names list of names
     * @param birthDates list of birthDates
     * @param addresses null list if not used
     * @param drivingPermits null list if not used
     * @param passports null list if not used
     * @param socialSecurityRecords list of social security records
     * @deprecated Do not use outside CRI-Lib, CRI's should use PersonIdentityDetailedFactory with
     *     the appropriate createPersonIdentityDetailedWith methods.
     * @see uk.gov.di.ipv.cri.common.library.service.PersonIdentityDetailedFactory
     *     createPersonIdentityDetailedWith
     */
    @JsonCreator
    @Deprecated(since = "1.5.0")
    public PersonIdentityDetailed(
            @JsonProperty("name") List<Name> names,
            @JsonProperty("birthDate") List<BirthDate> birthDates,
            @JsonProperty("address") List<Address> addresses,
            @JsonProperty("drivingPermit") List<DrivingPermit> drivingPermits,
            @JsonProperty("passport") List<Passport> passports,
            @JsonProperty("socialSecurityRecord")
                    List<SocialSecurityRecord> socialSecurityRecords) {
        this.names = names;
        this.birthDates = birthDates;
        this.addresses = addresses;
        this.drivingPermits = drivingPermits;
        this.passports = passports;
        this.socialSecurityRecords = socialSecurityRecords;
    }

    public List<SocialSecurityRecord> getSocialSecurityRecords() {
        return socialSecurityRecords;
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

    public List<Passport> getPassports() {
        return passports;
    }

    public DeviceInformation getDeviceInformation() {
        return deviceInformation;
    }

    public void setDeviceInformation(DeviceInformation deviceInformation) {
        this.deviceInformation = deviceInformation;
    }
}
