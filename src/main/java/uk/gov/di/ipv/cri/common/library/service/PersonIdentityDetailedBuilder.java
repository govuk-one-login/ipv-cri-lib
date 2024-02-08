package uk.gov.di.ipv.cri.common.library.service;

import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.DrivingPermit;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Passport;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;

import java.util.List;

public class PersonIdentityDetailedBuilder {
    /* Builder approach similar in intent to PersonIdentityDetailedFactory
    A way of constructing PersonIdentityDetailed where some components are not needed
    Avoids cris needing to provide null lists for parameters they don't need.
    Avoids a growing chain of lists being added to the public constructor and also
    useful in when used in conjunction with AuditContext to build out AuditEvent
    where names, birthDates may be desirable as nulls
    */
    private PersonIdentityDetailedBuilder() {
        throw new IllegalStateException("Instantiation is not valid for this class.");
    }

    public static Builder builder(List<Name> names, List<BirthDate> birthDates) {
        return new Builder(names, birthDates);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Name> names;
        private List<BirthDate> birthDates;
        private List<Address> addresses;
        private List<DrivingPermit> drivingPermits;
        private List<Passport> passports;

        private Builder() {
            this(null, null);
        }

        private Builder(List<Name> names, List<BirthDate> birthDates) {
            this.names = names;
            this.birthDates = birthDates;
        }

        public Builder withAddresses(List<Address> addresses) {
            this.addresses = addresses;
            return this;
        }

        public Builder withDrivingPermits(List<DrivingPermit> drivingPermits) {
            this.drivingPermits = drivingPermits;
            return this;
        }

        public Builder withPassports(List<Passport> passports) {
            this.passports = passports;
            return this;
        }
        @SuppressWarnings("deprecation")
        public PersonIdentityDetailed build() {
            return new PersonIdentityDetailed(
                    names, birthDates, addresses, drivingPermits, passports);
        }
    }
}
