package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.AddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.Name;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.SharedClaims;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityDateOfBirth;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityItem;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityName;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityNamePart;

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
        address.setValidFrom(TODAY);

        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        testPersonIdentityItem.setNames(List.of(name));
        testPersonIdentityItem.setBirthDates(List.of(birthDate));
        testPersonIdentityItem.setAddresses(List.of(address));

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
        assertEquals(AddressType.CURRENT, mappedAddress.getAddressType());
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
        address.setValidFrom(TODAY);
        sharedClaims.setAddresses(List.of(address));

        PersonIdentityItem mappedPersonIdentityItem =
                personIdentityMapper.mapToPersonIdentityItem(sharedClaims);

        PersonIdentityName mappedName = mappedPersonIdentityItem.getNames().get(0);
        CanonicalAddress mappedAddress = mappedPersonIdentityItem.getAddresses().get(0);
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
        assertEquals(TODAY, mappedAddress.getValidFrom());
        assertNull(mappedAddress.getValidUntil());
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

        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        testPersonIdentityItem.setNames(List.of(name));
        testPersonIdentityItem.setBirthDates(List.of(birthDate));
        testPersonIdentityItem.setAddresses(List.of(address));

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
                new PersonIdentityDetailed(
                        List.of(name), List.of(birthDate), List.of(address), null);

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
