package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.AddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.DrivingPermit;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Passport;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.SocialSecurityRecord;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonIdentityDetailedBuilderTest {
    @Nested
    class BuilderWithNamesParts {
        @Test
        void shouldUseBuilderToCreatePersonIdentityDetailedWithDrivingPermit() {

            NamePart firstNamePart = new NamePart();
            firstNamePart.setType("GivenName");
            firstNamePart.setValue("Jon");
            NamePart surnamePart = new NamePart();
            surnamePart.setType("FamilyName");
            surnamePart.setValue("Smith");
            Name name = new Name();
            name.setNameParts(List.of(firstNamePart, surnamePart));
            List<Name> names = List.of(name);

            LocalDate dob = LocalDate.of(1984, 6, 27);
            BirthDate birthDate = new BirthDate();
            birthDate.setValue(dob);
            List<BirthDate> birthDates = List.of(birthDate);

            // DP address is just postcode
            Address address = new Address();
            address.setPostalCode("postcode");
            List<Address> addresses = List.of(address);

            DrivingPermit drivingPermit = new DrivingPermit();
            drivingPermit.setPersonalNumber("123456789");
            drivingPermit.setIssueDate(dob.plus(18, ChronoUnit.YEARS).toString());
            drivingPermit.setExpiryDate(LocalDate.now().plus(10, ChronoUnit.YEARS).toString());
            drivingPermit.setIssuedBy("DVLA");
            drivingPermit.setIssueNumber("1");
            List<DrivingPermit> drivingPermits = List.of(drivingPermit);

            PersonIdentityDetailed personIdentityDetailed =
                    PersonIdentityDetailedBuilder.builder(names, birthDates)
                            .withAddresses(addresses)
                            .withDrivingPermits(drivingPermits)
                            .build();

            assertEquals(names, personIdentityDetailed.getNames());
            assertEquals(
                    firstNamePart.getValue(),
                    personIdentityDetailed.getNames().get(0).getNameParts().get(0).getValue());
            assertEquals(
                    surnamePart.getValue(),
                    personIdentityDetailed.getNames().get(0).getNameParts().get(1).getValue());

            assertEquals(birthDates, personIdentityDetailed.getBirthDates());
            assertEquals(
                    birthDate.getValue(), personIdentityDetailed.getBirthDates().get(0).getValue());

            Address pidAddress = personIdentityDetailed.getAddresses().get(0);
            assertEquals(addresses, personIdentityDetailed.getAddresses());
            assertEquals(address.getPostalCode(), pidAddress.getPostalCode());

            assertEquals(drivingPermits, personIdentityDetailed.getDrivingPermits());

            DrivingPermit pidDrivingPermit = personIdentityDetailed.getDrivingPermits().get(0);
            assertEquals(drivingPermit.getPersonalNumber(), pidDrivingPermit.getPersonalNumber());
            assertEquals(drivingPermit.getIssueDate(), pidDrivingPermit.getIssueDate());
            assertEquals(drivingPermit.getExpiryDate(), pidDrivingPermit.getExpiryDate());
            assertEquals(drivingPermit.getIssuedBy(), pidDrivingPermit.getIssuedBy());
            assertEquals(drivingPermit.getIssueNumber(), pidDrivingPermit.getIssueNumber());

            assertNull(personIdentityDetailed.getPassports());
            assertNull(personIdentityDetailed.getSocialSecurityRecords());
        }

        @Test
        void shouldUseBuilderToCreatePersonIdentityWithPassport() {

            NamePart firstNamePart = new NamePart();
            firstNamePart.setType("GivenName");
            firstNamePart.setValue("Jon");
            NamePart surnamePart = new NamePart();
            surnamePart.setType("FamilyName");
            surnamePart.setValue("Smith");
            Name name = new Name();
            name.setNameParts(List.of(firstNamePart, surnamePart));
            List<Name> names = List.of(name);

            LocalDate dob = LocalDate.of(1984, 6, 27);
            BirthDate birthDate = new BirthDate();
            birthDate.setValue(dob);
            List<BirthDate> birthDates = List.of(birthDate);

            Passport passport = new Passport();
            passport.setDocumentNumber("123456789");
            passport.setExpiryDate(LocalDate.now().plus(10, ChronoUnit.YEARS).toString());
            passport.setIcaoIssuerCode("GBR");
            List<Passport> passports = List.of(passport);

            PersonIdentityDetailed personIdentityDetailed =
                    PersonIdentityDetailedBuilder.builder(names, birthDates)
                            .withPassports(passports)
                            .build();

            assertEquals(names, personIdentityDetailed.getNames());
            assertEquals(
                    firstNamePart.getValue(),
                    personIdentityDetailed.getNames().get(0).getNameParts().get(0).getValue());
            assertEquals(
                    surnamePart.getValue(),
                    personIdentityDetailed.getNames().get(0).getNameParts().get(1).getValue());

            assertEquals(birthDates, personIdentityDetailed.getBirthDates());
            assertEquals(
                    birthDate.getValue(), personIdentityDetailed.getBirthDates().get(0).getValue());

            Passport pidPassport = personIdentityDetailed.getPassports().get(0);
            assertEquals(passports, personIdentityDetailed.getPassports());
            assertEquals(passport.getDocumentNumber(), pidPassport.getDocumentNumber());
            assertEquals(passport.getExpiryDate(), pidPassport.getExpiryDate());
            assertEquals(passport.getIcaoIssuerCode(), pidPassport.getIcaoIssuerCode());

            assertNull(personIdentityDetailed.getAddresses());
            assertNull(personIdentityDetailed.getDrivingPermits());
            assertNull(personIdentityDetailed.getSocialSecurityRecords());
        }

        @Test
        void shouldUseBuilderToCreatePersonIdentityWithNino() {
            SocialSecurityRecord socialSecurityRecord = new SocialSecurityRecord();
            socialSecurityRecord.setPersonalNumber("AA000003D");
            List<SocialSecurityRecord> socialSecurityRecords = List.of(socialSecurityRecord);

            NamePart firstNamePart = new NamePart();
            firstNamePart.setType("GivenName");
            firstNamePart.setValue("Jon");
            NamePart surnamePart = new NamePart();
            surnamePart.setType("FamilyName");
            surnamePart.setValue("Smith");
            Name name = new Name();
            name.setNameParts(List.of(firstNamePart, surnamePart));
            List<Name> names = List.of(name);

            LocalDate dob = LocalDate.of(1984, 6, 27);
            BirthDate birthDate = new BirthDate();
            birthDate.setValue(dob);
            List<BirthDate> birthDates = List.of(birthDate);

            PersonIdentityDetailed personIdentityDetailed =
                    PersonIdentityDetailedBuilder.builder(names, birthDates)
                            .withNino(socialSecurityRecords)
                            .build();

            assertEquals(names, personIdentityDetailed.getNames());
            assertEquals(
                    firstNamePart.getValue(),
                    personIdentityDetailed.getNames().get(0).getNameParts().get(0).getValue());
            assertEquals(
                    surnamePart.getValue(),
                    personIdentityDetailed.getNames().get(0).getNameParts().get(1).getValue());

            assertEquals(birthDates, personIdentityDetailed.getBirthDates());
            assertEquals(
                    birthDate.getValue(), personIdentityDetailed.getBirthDates().get(0).getValue());

            SocialSecurityRecord personSocialSecurityRecord =
                    personIdentityDetailed.getSocialSecurityRecords().get(0);
            assertEquals(
                    personSocialSecurityRecord.getPersonalNumber(),
                    socialSecurityRecords.get(0).getPersonalNumber());

            assertNull(personIdentityDetailed.getAddresses());
            assertNull(personIdentityDetailed.getDrivingPermits());
        }
    }

    @Nested
    class BuilderWithoutNamesParts {
        @Test
        void shouldUseBuilderToCreatePersonIdentityDetailedWithAddresses() {
            Address address = new Address();
            address.setBuildingNumber("buildingNum");
            address.setBuildingName("buildingName");
            address.setStreetName("street");
            address.setAddressLocality("locality");
            address.setPostalCode("postcode");
            address.setValidFrom(LocalDate.now());

            List<Address> addresses = List.of(address);
            PersonIdentityDetailed personIdentityDetailed =
                    PersonIdentityDetailedBuilder.builder().withAddresses(addresses).build();

            Address pidAddress = personIdentityDetailed.getAddresses().get(0);
            assertEquals(addresses, personIdentityDetailed.getAddresses());
            assertEquals(address.getBuildingName(), pidAddress.getBuildingName());
            assertEquals(address.getBuildingNumber(), pidAddress.getBuildingNumber());
            assertEquals(address.getStreetName(), pidAddress.getStreetName());
            assertEquals(address.getAddressLocality(), pidAddress.getAddressLocality());
            assertEquals(address.getPostalCode(), pidAddress.getPostalCode());
            assertEquals(address.getValidFrom(), pidAddress.getValidFrom());
            assertEquals(AddressType.CURRENT, pidAddress.getAddressType());

            assertNull(personIdentityDetailed.getDrivingPermits());
            assertNull(personIdentityDetailed.getPassports());
        }
    }
}
