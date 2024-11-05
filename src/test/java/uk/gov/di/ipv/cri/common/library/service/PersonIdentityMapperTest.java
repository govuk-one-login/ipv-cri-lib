package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.AddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.DrivingPermit;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.SharedClaims;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.SocialSecurityRecord;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityDateOfBirth;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityDrivingPermit;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityItem;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityName;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityNamePart;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentitySocialSecurityRecord;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PersonIdentityMapperTest {
    private static final LocalDate TODAY = LocalDate.now();
    private PersonIdentityMapper personIdentityMapper;

    @BeforeEach()
    void setup() {
        this.personIdentityMapper = new PersonIdentityMapper();
    }

    @Test
    void shouldMapToPersonIdentity() {
        PersonIdentityNamePart firstNamePart = new PersonIdentityNamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Jon");
        PersonIdentityNamePart surnamePart = new PersonIdentityNamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Smith");
        PersonIdentityName name = new PersonIdentityName();
        name.setNameParts(List.of(firstNamePart, surnamePart));

        PersonIdentityDateOfBirth birthDate = new PersonIdentityDateOfBirth();
        birthDate.setValue(LocalDate.of(1980, 10, 20));

        CanonicalAddress address = new CanonicalAddress();
        address.setBuildingNumber("buildingNum");
        address.setBuildingName("buildingName");
        address.setStreetName("street");
        address.setAddressLocality("locality");
        address.setPostalCode("postcode");
        address.setAddressRegion("dummyRegion");
        address.setValidFrom(TODAY);

        PersonIdentitySocialSecurityRecord socialSecurityRecord =
                new PersonIdentitySocialSecurityRecord();
        socialSecurityRecord.setPersonalNumber("AA000003D");

        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        testPersonIdentityItem.setNames(List.of(name));
        testPersonIdentityItem.setBirthDates(List.of(birthDate));
        testPersonIdentityItem.setAddresses(List.of(address));
        testPersonIdentityItem.setSocialSecurityRecords(List.of(socialSecurityRecord));

        PersonIdentity mappedPersonIdentity =
                this.personIdentityMapper.mapToPersonIdentity(testPersonIdentityItem);

        assertEquals(firstNamePart.getValue(), mappedPersonIdentity.getFirstName());
        assertEquals(surnamePart.getValue(), mappedPersonIdentity.getSurname());
        assertEquals(birthDate.getValue(), mappedPersonIdentity.getDateOfBirth());
        Address mappedAddress = mappedPersonIdentity.getAddresses().get(0);
        assertEquals(address.getBuildingName(), mappedAddress.getBuildingName());
        assertEquals(address.getBuildingNumber(), mappedAddress.getBuildingNumber());
        assertEquals(address.getStreetName(), mappedAddress.getStreetName());
        assertEquals(address.getAddressLocality(), mappedAddress.getAddressLocality());
        assertEquals(address.getPostalCode(), mappedAddress.getPostalCode());
        assertEquals(address.getValidFrom(), mappedAddress.getValidFrom());
        assertEquals(address.getAddressRegion(), mappedAddress.getAddressRegion());
        assertEquals(AddressType.CURRENT, mappedAddress.getAddressType());
        assertEquals(
                socialSecurityRecord.getPersonalNumber(),
                testPersonIdentityItem.getSocialSecurityRecords().get(0).getPersonalNumber());
    }

    @Test
    void shouldMapMiddleNames() {
        PersonIdentityNamePart firstNamePart = new PersonIdentityNamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Jon");
        PersonIdentityNamePart secondNamePart = new PersonIdentityNamePart();
        secondNamePart.setType("GivenName");
        secondNamePart.setValue("Henry");
        PersonIdentityNamePart thirdNamePart = new PersonIdentityNamePart();
        thirdNamePart.setType("GivenName");
        thirdNamePart.setValue("Jack");
        PersonIdentityNamePart surnamePart = new PersonIdentityNamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Smith");
        PersonIdentityName name = new PersonIdentityName();
        name.setNameParts(List.of(firstNamePart, secondNamePart, thirdNamePart, surnamePart));
        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        testPersonIdentityItem.setNames(List.of(name));

        PersonIdentity mappedPersonIdentity =
                this.personIdentityMapper.mapToPersonIdentity(testPersonIdentityItem);

        assertEquals(firstNamePart.getValue(), mappedPersonIdentity.getFirstName());
        assertEquals(
                secondNamePart.getValue() + " " + thirdNamePart.getValue(),
                mappedPersonIdentity.getMiddleNames());
        assertEquals(surnamePart.getValue(), mappedPersonIdentity.getSurname());
        assertNull(mappedPersonIdentity.getDateOfBirth());
        assertEquals(0, mappedPersonIdentity.getAddresses().size());
    }

    @Test
    void shouldThrowExceptionWhenMappingMultipleNames() {
        PersonIdentityNamePart firstNamePart = new PersonIdentityNamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Sarah");
        PersonIdentityNamePart surnamePart = new PersonIdentityNamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Jones");
        PersonIdentityName previousName = new PersonIdentityName();
        previousName.setNameParts(List.of(firstNamePart, surnamePart));

        PersonIdentityNamePart currentFirstNamePart = new PersonIdentityNamePart();
        currentFirstNamePart.setType("GivenName");
        currentFirstNamePart.setValue("Sarah");
        PersonIdentityNamePart currentSurnamePart = new PersonIdentityNamePart();
        currentSurnamePart.setType("FamilyName");
        currentSurnamePart.setValue("Young");
        PersonIdentityName currentName = new PersonIdentityName();
        currentName.setNameParts(List.of(currentFirstNamePart, currentSurnamePart));

        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        testPersonIdentityItem.setNames(List.of(previousName, currentName));

        assertThrows(
                IllegalArgumentException.class,
                () -> personIdentityMapper.mapToPersonIdentity(testPersonIdentityItem),
                "Unable to map person identity with multiple names");
    }

    @Test
    void shouldMapCurrentAndPreviousAddresses() {
        // address
        CanonicalAddress address = new CanonicalAddress();
        address.setBuildingNumber("buildingNum");
        address.setStreetName("street");
        address.setPostalCode("postcode");
        address.setValidFrom(TODAY);

        CanonicalAddress previousAddress = new CanonicalAddress();
        previousAddress.setBuildingNumber("buildingNum");
        previousAddress.setStreetName("street");
        previousAddress.setPostalCode("postcode");
        previousAddress.setAddressRegion("dummyRegion");
        previousAddress.setValidUntil(TODAY.minus(1L, ChronoUnit.DAYS));

        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        testPersonIdentityItem.setAddresses(List.of(address, previousAddress));

        PersonIdentity mappedPersonIdentity =
                personIdentityMapper.mapToPersonIdentity(testPersonIdentityItem);

        assertEquals(2, mappedPersonIdentity.getAddresses().size());
        assertEquals(
                AddressType.CURRENT, mappedPersonIdentity.getAddresses().get(0).getAddressType());
        assertEquals(
                AddressType.PREVIOUS, mappedPersonIdentity.getAddresses().get(1).getAddressType());
    }

    @Test
    void shouldMapSharedClaimsToPersonIdentityItem() {
        SharedClaims sharedClaims = new SharedClaims();

        NamePart firstNamePart = new NamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Jon");
        NamePart surnamePart = new NamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Smith");
        Name name = new Name();
        name.setNameParts(List.of(firstNamePart, surnamePart));
        sharedClaims.setNames(List.of(name));

        BirthDate birthDate = new BirthDate();
        birthDate.setValue(LocalDate.of(1984, 6, 27));
        sharedClaims.setBirthDates(List.of(birthDate));

        Address address = new Address();
        address.setBuildingNumber("buildingNum");
        address.setBuildingName("buildingName");
        address.setStreetName("street");
        address.setAddressLocality("locality");
        address.setPostalCode("postcode");
        address.setAddressRegion("dummyRegion");
        address.setValidFrom(TODAY);
        sharedClaims.setAddresses(List.of(address));

        SocialSecurityRecord socialSecurityRecord = new SocialSecurityRecord();
        socialSecurityRecord.setPersonalNumber("AA000003D");
        sharedClaims.setSocialSecurityRecords(List.of(socialSecurityRecord));

        PersonIdentityItem mappedPersonIdentityItem =
                personIdentityMapper.mapToPersonIdentityItem(sharedClaims);

        PersonIdentityName mappedName = mappedPersonIdentityItem.getNames().get(0);
        CanonicalAddress mappedAddress = mappedPersonIdentityItem.getAddresses().get(0);
        PersonIdentitySocialSecurityRecord mappedSocialSecurityRecord =
                mappedPersonIdentityItem.getSocialSecurityRecords().get(0);

        assertEquals(firstNamePart.getValue(), mappedName.getNameParts().get(0).getValue());
        assertEquals(firstNamePart.getType(), mappedName.getNameParts().get(0).getType());
        assertEquals(surnamePart.getValue(), mappedName.getNameParts().get(1).getValue());
        assertEquals(surnamePart.getType(), mappedName.getNameParts().get(1).getType());
        assertEquals(
                birthDate.getValue(), mappedPersonIdentityItem.getBirthDates().get(0).getValue());
        assertEquals(address.getAddressLocality(), mappedAddress.getAddressLocality());
        assertEquals(address.getBuildingName(), mappedAddress.getBuildingName());
        assertEquals(address.getBuildingNumber(), mappedAddress.getBuildingNumber());
        assertEquals(address.getStreetName(), mappedAddress.getStreetName());
        assertEquals(address.getPostalCode(), mappedAddress.getPostalCode());
        assertEquals(address.getAddressRegion(), mappedAddress.getAddressRegion());
        assertEquals(TODAY, mappedAddress.getValidFrom());
        assertNull(mappedAddress.getValidUntil());
        assertEquals(
                socialSecurityRecord.getPersonalNumber(),
                mappedSocialSecurityRecord.getPersonalNumber());
    }

    @ParameterizedTest
    @CsvSource({
        "DVA,,", // No full address
        // Just postcodes
        "DVA, 'BT11AB', BT11AB", // Edgecase full address is just a 6 char postcode
        "DVA, 'BT1 1AB', BT1 1AB", // Edgecase full address is just a 6 char postcode mid space
        "DVA, ',BT11AB', BT11AB", // Edgecase OCR failure, 6 char postcode with comma
        "DVA, ',BT1 1AB', BT1 1AB", // Edgecase as above but with space
        "DVA, ',BT121AB', BT121AB", // Edgecase OCR failure, 7 char postcode with comma
        "DVA, ',BT12 1AB', BT12 1AB", // Edgecase as above but with space
        "DVA, 'BT11 1AB', BT11 1AB", // 8 Exactly
        "DVA, ',BT11 1AB', BT11 1AB", // 8 Exactly, with leading comma
        // Full Address
        "DVA, 'Building, Road, Town, County, BT11AB', BT11AB", // 6Char postcode/ address-commas
        "DVA, 'Building Road Town County BT11AB',Y BT11AB", // 6Char postcode/ address-spaces
        "DVA, 'Building, Road, Town, County, BT1 1AB', BT1 1AB", // 7Char postcode/ address-commas
        "DVA, 'Building Road Town County BT1 1AB',BT1 1AB", // 7Char postcode address-spaces
        "DVA, 'Building, Road, Town, County, BT121AB', BT121AB", // 7Char postcode address-commas
        "DVA, 'Building Road Town County BT12 1AB',BT12 1AB", // 7Char postcode/ address-spaces
        "DVA, 'Building, Road, Town, County, BT12 1AB', BT12 1AB", // 8Char postcode/ Address commas
        "DVA, 'Building Road Town County BT12 1AB',BT12 1AB", // 8Char postcode, address-spaces
        // DVA No postcode Tests
        "DVA, 'Building, Road, Town, County', COUNTY", // No postcode / Address commas
        "DVA, 'Building Road Town County', N COUNTY", // No postcode/ Address spaces
        // DVLA
        "DVLA,,", // No full address
        // Just postcodes
        "DVLA, 'AB11AB', AB11AB", // Edgecase full address is just a 6 char postcode
        "DVLA, 'AB1 1AB', AB1 1AB", // Edgecase full address is just a 6 char postcode mid space
        "DVLA, ',AB11AB', AB11AB", // Edgecase OCR failure, 6 char postcode with comma
        "DVLA, ',AB1 1AB', AB1 1AB", // Edgecase as above but with space
        "DVLA, ',AB121AB', AB121AB", // Edgecase OCR failure, 7 char postcode with comma
        "DVLA, ',AB12 1AB', AB12 1AB", // Edgecase as above but with space
        "DVLA, 'AB11 1AB', AB11 1AB", // 8 Exactly
        "DVLA, ',AB11 1AB', AB11 1AB", // 8 Exactly, with leading comma
        // DVLA Full Address
        "DVLA, 'Building, Road, Town, County, AB11AB', AB11AB", // 6Char postcode/ address-commas
        "DVLA, 'Building Road Town County AB11AB',Y AB11AB", // 6Char postcode/ address-spaces
        "DVLA, 'Building, Road, Town, County, AB1 1AB', AB1 1AB", // 7Char postcode/ address-commas
        "DVLA, 'Building Road Town County AB1 1AB',AB1 1AB", // 7Char postcode address-spaces
        "DVLA, 'Building, Road, Town, County, AB121AB', AB121AB", // 7Char postcode address-commas
        "DVLA, 'Building Road Town County AB12 1AB',AB12 1AB", // 7Char postcode/ address-spaces
        "DVLA, 'Building, Road, Town, County, AB12 1AB', AB12 1AB", // 8Char postcode/
        // address-commas
        "DVLA, 'Building Road Town County AB12 1AB',AB12 1AB", // 8Char postcode, address-spaces
        // DVLA No postcode Tests
        "DVLA, 'Building, Road, Town, County,', 'COUNTY,'", // No postcode / Address-commas
        "DVLA, 'Building Road Town County', 'N COUNTY'", // No postcode/ Address-spaces
    })
    void shouldMapDrivingPermitSharedClaimsToPersonIdentityItem(
            String issuer, String fullAddress, String expectedPostcode) {
        SharedClaims sharedClaims = new SharedClaims();

        NamePart firstNamePart = new NamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Jon");
        NamePart surnamePart = new NamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Smith");
        Name name = new Name();
        name.setNameParts(List.of(firstNamePart, surnamePart));
        sharedClaims.setNames(List.of(name));

        BirthDate birthDate = new BirthDate();
        birthDate.setValue(LocalDate.of(1984, 6, 27));
        sharedClaims.setBirthDates(List.of(birthDate));

        DrivingPermit drivingPermit = new DrivingPermit();
        drivingPermit.setPersonalNumber("personalNumber");
        drivingPermit.setExpiryDate(LocalDate.of(2029, 10, 21).toString());
        drivingPermit.setIssueDate(LocalDate.of(2011, 10, 21).toString());
        drivingPermit.setIssueNumber("issueNumber");
        drivingPermit.setIssuedBy(issuer);

        drivingPermit.setFullAddress(fullAddress);

        sharedClaims.setDrivingPermits(List.of(drivingPermit));

        PersonIdentityItem mappedPersonIdentityItem =
                personIdentityMapper.mapToPersonIdentityItem(sharedClaims);

        PersonIdentityName mappedName = mappedPersonIdentityItem.getNames().get(0);
        CanonicalAddress mappedAddress = null;
        if (!mappedPersonIdentityItem.getAddresses().isEmpty()) {
            mappedAddress = mappedPersonIdentityItem.getAddresses().get(0);
        }

        PersonIdentityDrivingPermit mappedDrivingPermit =
                mappedPersonIdentityItem.getDrivingPermits().get(0);

        assertEquals(firstNamePart.getValue(), mappedName.getNameParts().get(0).getValue());
        assertEquals(firstNamePart.getType(), mappedName.getNameParts().get(0).getType());
        assertEquals(surnamePart.getValue(), mappedName.getNameParts().get(1).getValue());
        assertEquals(surnamePart.getType(), mappedName.getNameParts().get(1).getType());
        assertEquals(
                birthDate.getValue(), mappedPersonIdentityItem.getBirthDates().get(0).getValue());

        // Address will be created from the full address and will include just the postcode
        if (mappedAddress != null) {
            assertNull(mappedAddress.getAddressLocality());
            assertNull(mappedAddress.getBuildingName());
            assertNull(mappedAddress.getBuildingNumber());
            assertNull(mappedAddress.getStreetName());
            assertEquals(expectedPostcode, mappedAddress.getPostalCode());
            assertNull(mappedAddress.getAddressRegion());
            assertNull(mappedAddress.getValidFrom());
            assertNull(mappedAddress.getValidUntil());
        } else {
            assertNull(drivingPermit.getFullAddress());
        }

        assertEquals(drivingPermit.getPersonalNumber(), mappedDrivingPermit.getPersonalNumber());
        assertEquals(drivingPermit.getExpiryDate(), mappedDrivingPermit.getExpiryDate());
        assertEquals(drivingPermit.getIssueDate(), mappedDrivingPermit.getIssueDate());
        assertEquals(drivingPermit.getIssueNumber(), mappedDrivingPermit.getIssueNumber());
        assertEquals(drivingPermit.getIssuedBy(), mappedDrivingPermit.getIssuedBy());
        assertEquals(drivingPermit.getFullAddress(), mappedDrivingPermit.getFullAddress());
    }

    @Test
    void shouldMapPersonIdentityItemToPersonIdentityDetailed() {
        PersonIdentityNamePart firstNamePart = new PersonIdentityNamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Jon");
        PersonIdentityNamePart surnamePart = new PersonIdentityNamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Smith");
        PersonIdentityName name = new PersonIdentityName();
        name.setNameParts(List.of(firstNamePart, surnamePart));

        PersonIdentityDateOfBirth birthDate = new PersonIdentityDateOfBirth();
        birthDate.setValue(LocalDate.of(1980, 10, 20));

        CanonicalAddress address = new CanonicalAddress();
        address.setAddressCountry("GB");
        address.setAddressRegion("dummyRegion");
        address.setAddressLocality("locality");
        address.setBuildingNumber("buildingNum");
        address.setBuildingName("buildingName");
        address.setDepartmentName("deptName");
        address.setDependentAddressLocality("depAddressLocality");
        address.setDependentStreetName("depStreetName");
        address.setDoubleDependentAddressLocality("doubleDepAddressLocality");
        address.setOrganisationName("orgName");
        address.setPostalCode("postcode");
        address.setStreetName("street");
        address.setSubBuildingName("subBuildingName");
        address.setUprn(2394501657L);
        address.setValidFrom(LocalDate.of(2011, 10, 21));
        address.setValidUntil(LocalDate.of(2017, 11, 25));

        PersonIdentitySocialSecurityRecord personIdentitySocialSecurityRecord =
                new PersonIdentitySocialSecurityRecord();
        personIdentitySocialSecurityRecord.setPersonalNumber("AA000003D");

        PersonIdentityDrivingPermit personIdentityDrivingPermit = new PersonIdentityDrivingPermit();
        personIdentityDrivingPermit.setPersonalNumber("personalNumber");
        personIdentityDrivingPermit.setExpiryDate(LocalDate.of(2029, 10, 21).toString());
        personIdentityDrivingPermit.setIssueDate(LocalDate.of(2011, 10, 21).toString());
        personIdentityDrivingPermit.setIssueNumber("issueNumber");
        personIdentityDrivingPermit.setIssuedBy("issuedBy");
        personIdentityDrivingPermit.setFullAddress("fullAddress");

        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        testPersonIdentityItem.setNames(List.of(name));
        testPersonIdentityItem.setBirthDates(List.of(birthDate));
        testPersonIdentityItem.setAddresses(List.of(address));
        testPersonIdentityItem.setSocialSecurityRecords(
                List.of(personIdentitySocialSecurityRecord));
        testPersonIdentityItem.setDrivingPermits(List.of(personIdentityDrivingPermit));

        PersonIdentityDetailed mappedPersonIdentity =
                personIdentityMapper.mapToPersonIdentityDetailed(testPersonIdentityItem);

        List<NamePart> mappedNameParts = mappedPersonIdentity.getNames().get(0).getNameParts();
        assertEquals(firstNamePart.getValue(), mappedNameParts.get(0).getValue());
        assertEquals(firstNamePart.getType(), mappedNameParts.get(0).getType());
        assertEquals(surnamePart.getValue(), mappedNameParts.get(1).getValue());
        assertEquals(surnamePart.getType(), mappedNameParts.get(1).getType());
        assertEquals(birthDate.getValue(), mappedPersonIdentity.getBirthDates().get(0).getValue());
        Address mappedAddress = mappedPersonIdentity.getAddresses().get(0);
        assertEquals(address.getAddressCountry(), mappedAddress.getAddressCountry());
        assertEquals(address.getAddressRegion(), mappedAddress.getAddressRegion());
        assertEquals(address.getAddressLocality(), mappedAddress.getAddressLocality());

        assertEquals(address.getBuildingNumber(), mappedAddress.getBuildingNumber());
        assertEquals(address.getBuildingName(), mappedAddress.getBuildingName());
        assertEquals(address.getDepartmentName(), mappedAddress.getDepartmentName());
        assertEquals(
                address.getDependentAddressLocality(), mappedAddress.getDependentAddressLocality());
        assertEquals(address.getDependentStreetName(), mappedAddress.getDependentStreetName());
        assertEquals(
                address.getDoubleDependentAddressLocality(),
                mappedAddress.getDoubleDependentAddressLocality());
        assertEquals(address.getOrganisationName(), mappedAddress.getOrganisationName());
        assertEquals(address.getPostalCode(), mappedAddress.getPostalCode());
        assertEquals(address.getStreetName(), mappedAddress.getStreetName());
        assertEquals(address.getSubBuildingName(), mappedAddress.getSubBuildingName());
        assertEquals(address.getUprn(), mappedAddress.getUprn());

        DrivingPermit mappedDrivingPermit = mappedPersonIdentity.getDrivingPermits().get(0);
        assertEquals(
                personIdentityDrivingPermit.getPersonalNumber(),
                mappedDrivingPermit.getPersonalNumber());
        assertEquals(
                personIdentityDrivingPermit.getExpiryDate(), mappedDrivingPermit.getExpiryDate());
        assertEquals(
                personIdentityDrivingPermit.getIssueDate(), mappedDrivingPermit.getIssueDate());
        assertEquals(
                personIdentityDrivingPermit.getIssueNumber(), mappedDrivingPermit.getIssueNumber());
        assertEquals(personIdentityDrivingPermit.getIssuedBy(), mappedDrivingPermit.getIssuedBy());
        assertEquals(
                personIdentityDrivingPermit.getFullAddress(), mappedDrivingPermit.getFullAddress());

        // CRIs using a java backend currently shouldn't have a nino sent to them,
        // functionality has been added in case this changes in the future but for now
        // this checks the value hasn't been mapped into the personIdentity
        assertNull(mappedPersonIdentity.getSocialSecurityRecords());
    }

    @Test
    void shouldMapPersonIdentityDetailedToPersonIdentity() {
        NamePart firstNamePart = new NamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Jon");
        NamePart middleNamePart = new NamePart();
        middleNamePart.setType("GivenName");
        middleNamePart.setValue("Alexander");
        NamePart surnamePart = new NamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Smith");
        Name name = new Name();
        name.setNameParts(List.of(firstNamePart, middleNamePart, surnamePart));

        BirthDate birthDate = new BirthDate();
        birthDate.setValue(LocalDate.of(1980, 10, 20));

        Address address = new Address();
        address.setBuildingNumber("buildingNum");
        address.setBuildingName("buildingName");
        address.setStreetName("street");
        address.setAddressLocality("locality");
        address.setPostalCode("postcode");
        address.setValidFrom(TODAY);

        PersonIdentityDetailed testPersonIdentity =
                PersonIdentityDetailedFactory.createPersonIdentityDetailedWithAddresses(
                        List.of(name), List.of(birthDate), List.of(address));

        PersonIdentity mappedPersonIdentity =
                this.personIdentityMapper.mapToPersonIdentity(testPersonIdentity);

        assertEquals(firstNamePart.getValue(), mappedPersonIdentity.getFirstName());
        assertEquals(middleNamePart.getValue(), mappedPersonIdentity.getMiddleNames());
        assertEquals(surnamePart.getValue(), mappedPersonIdentity.getSurname());
        assertEquals(birthDate.getValue(), mappedPersonIdentity.getDateOfBirth());
        Address mappedAddress = mappedPersonIdentity.getAddresses().get(0);
        assertEquals(address.getBuildingName(), mappedAddress.getBuildingName());
        assertEquals(address.getBuildingNumber(), mappedAddress.getBuildingNumber());
        assertEquals(address.getStreetName(), mappedAddress.getStreetName());
        assertEquals(address.getAddressLocality(), mappedAddress.getAddressLocality());
        assertEquals(address.getPostalCode(), mappedAddress.getPostalCode());
        assertEquals(address.getValidFrom(), mappedAddress.getValidFrom());
        assertEquals(AddressType.CURRENT, mappedAddress.getAddressType());
    }
}
