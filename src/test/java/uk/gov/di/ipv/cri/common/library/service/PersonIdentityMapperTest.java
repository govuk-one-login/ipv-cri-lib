package uk.gov.di.ipv.cri.common.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonAddress;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonAddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.Address;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.Name;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.NamePart;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.SharedClaims;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityDateOfBirth;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityItem;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityName;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityNamePart;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        PersonAddress mappedAddress = mappedPersonIdentity.getAddresses().get(0);
        assertEquals(address.getBuildingName(), mappedAddress.getBuildingName());
        assertEquals(address.getBuildingNumber(), mappedAddress.getBuildingNumber());
        assertEquals(address.getStreetName(), mappedAddress.getStreet());
        assertEquals(address.getAddressLocality(), mappedAddress.getTownCity());
        assertEquals(address.getPostalCode(), mappedAddress.getPostcode());
        assertEquals(address.getValidFrom(), mappedAddress.getDateMovedIn());
        assertEquals(PersonAddressType.CURRENT, mappedAddress.getAddressType());
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
    void shouldMapCurrentName() {
        LocalDate validityDate = LocalDate.of(2011, 5, 20);
        PersonIdentityNamePart firstNamePart = new PersonIdentityNamePart();
        firstNamePart.setType("GivenName");
        firstNamePart.setValue("Sarah");
        PersonIdentityNamePart surnamePart = new PersonIdentityNamePart();
        surnamePart.setType("FamilyName");
        surnamePart.setValue("Jones");
        PersonIdentityName previousName = new PersonIdentityName();
        previousName.setNameParts(List.of(firstNamePart, surnamePart));
        previousName.setValidFrom(LocalDate.of(1980, 5, 24));
        previousName.setValidUntil(validityDate);

        PersonIdentityNamePart currentFirstNamePart = new PersonIdentityNamePart();
        currentFirstNamePart.setType("GivenName");
        currentFirstNamePart.setValue("Sarah");
        PersonIdentityNamePart currentSurnamePart = new PersonIdentityNamePart();
        currentSurnamePart.setType("FamilyName");
        currentSurnamePart.setValue("Young");
        PersonIdentityName currentName = new PersonIdentityName();
        currentName.setNameParts(List.of(currentFirstNamePart, currentSurnamePart));
        currentName.setValidFrom(validityDate);

        PersonIdentityItem testPersonIdentityItem = new PersonIdentityItem();
        testPersonIdentityItem.setNames(List.of(previousName, currentName));

        PersonIdentity mappedPersonIdentity =
                personIdentityMapper.mapToPersonIdentity(testPersonIdentityItem);

        assertEquals(currentFirstNamePart.getValue(), mappedPersonIdentity.getFirstName());
        assertEquals(currentSurnamePart.getValue(), mappedPersonIdentity.getSurname());
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
                PersonAddressType.CURRENT,
                mappedPersonIdentity.getAddresses().get(0).getAddressType());
        assertEquals(
                PersonAddressType.PREVIOUS,
                mappedPersonIdentity.getAddresses().get(1).getAddressType());
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
        name.setValidFrom(TODAY);
        name.setNameParts(List.of(firstNamePart, surnamePart));
        sharedClaims.setNames(List.of(name));

        BirthDate birthDate = new BirthDate();
        birthDate.setValue(LocalDate.of(1984, 6, 27).toString());
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
        assertEquals(name.getValidFrom(), mappedName.getValidFrom());
        assertEquals(firstNamePart.getValue(), mappedName.getNameParts().get(0).getValue());
        assertEquals(firstNamePart.getType(), mappedName.getNameParts().get(0).getType());
        assertEquals(surnamePart.getValue(), mappedName.getNameParts().get(1).getValue());
        assertEquals(surnamePart.getType(), mappedName.getNameParts().get(1).getType());
        assertEquals(
                LocalDate.parse(birthDate.getValue()),
                mappedPersonIdentityItem.getBirthDates().get(0).getValue());
        assertEquals(address.getAddressLocality(), mappedAddress.getAddressLocality());
        assertEquals(address.getBuildingName(), mappedAddress.getBuildingName());
        assertEquals(address.getBuildingNumber(), mappedAddress.getBuildingNumber());
        assertEquals(address.getStreetName(), mappedAddress.getStreetName());
        assertEquals(address.getPostalCode(), mappedAddress.getPostalCode());
        assertEquals(TODAY, mappedAddress.getValidFrom());
        assertNull(mappedAddress.getValidUntil());
    }
}
