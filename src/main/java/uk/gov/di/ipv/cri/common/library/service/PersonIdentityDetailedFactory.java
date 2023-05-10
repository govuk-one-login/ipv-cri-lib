package uk.gov.di.ipv.cri.common.library.service;

import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.DrivingPermit;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Passport;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;

import java.util.List;

public class PersonIdentityDetailedFactory {

    private PersonIdentityDetailedFactory() {
        throw new IllegalStateException("Instantiation is not valid for this class.");
    }

    // Factory methods to work around type erasure on constructor lists in PersonIdentityDetailed
    // Avoids cris needing to provide null lists they don't use
    // Avoids a growing chain of lists being added to the public constructor (and then needing
    // updated in CRI's)
    // Limits updates to this file.
    @SuppressWarnings("deprecation")
    public static PersonIdentityDetailed createPersonIdentityDetailedWithAddresses(
            List<Name> names, List<BirthDate> birthDates, List<Address> addresses) {
        return new PersonIdentityDetailed(names, birthDates, addresses, null, null);
    }

    @SuppressWarnings("deprecation")
    public static PersonIdentityDetailed createPersonIdentityDetailedWithDrivingPermit(
            List<Name> names,
            List<BirthDate> birthDates,
            List<Address> addresses,
            List<DrivingPermit> drivingPermits) {
        return new PersonIdentityDetailed(names, birthDates, addresses, drivingPermits, null);
    }

    @SuppressWarnings("deprecation")
    public static PersonIdentityDetailed createPersonIdentityDetailedWithPassport(
            List<Name> names, List<BirthDate> birthDates, List<Passport> passports) {
        return new PersonIdentityDetailed(names, birthDates, null, null, passports);
    }
}
