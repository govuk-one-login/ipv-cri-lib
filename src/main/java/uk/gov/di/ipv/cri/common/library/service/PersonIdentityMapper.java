package uk.gov.di.ipv.cri.common.library.service;

import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

class PersonIdentityMapper {

    private enum NamePartType {
        GIVEN_NAME("GivenName"),
        FAMILY_NAME("FamilyName");

        private final String value;

        NamePartType(String value) {
            this.value = value;
        }
    }

    PersonIdentityItem mapToPersonIdentityItem(SharedClaims sharedClaims) {
        PersonIdentityItem identity = new PersonIdentityItem();
        if (notNullAndNotEmpty(sharedClaims.getBirthDates())) {
            identity.setBirthDates(mapBirthDates(sharedClaims.getBirthDates()));
        }
        if (notNullAndNotEmpty(sharedClaims.getNames())) {
            identity.setNames(mapNames(sharedClaims.getNames()));
        }
        if (notNullAndNotEmpty(sharedClaims.getAddresses())) {
            identity.setAddresses(mapAddresses(sharedClaims.getAddresses()));
        }
        return identity;
    }

    PersonIdentity mapToPersonIdentity(PersonIdentityItem personIdentityItem) {
        PersonIdentity personIdentity = new PersonIdentity();

        if (notNullAndNotEmpty(personIdentityItem.getNames())) {
            PersonIdentityName personIdentityName = getCurrentName(personIdentityItem.getNames());
            mapName(personIdentityName, personIdentity);
        }

        if (notNullAndNotEmpty(personIdentityItem.getBirthDates())) {
            personIdentity.setDateOfBirth(personIdentityItem.getBirthDates().get(0).getValue());
        }

        if (notNullAndNotEmpty(personIdentityItem.getAddresses())) {
            personIdentity.setAddresses(mapCanonicalAddresses(personIdentityItem.getAddresses()));
        }

        return personIdentity;
    }

    PersonIdentity mapToPersonIdentity(PersonIdentityDetailed personIdentityDetailed) {
        PersonIdentity personIdentity = new PersonIdentity();
        if (notNullAndNotEmpty(personIdentityDetailed.getNames())) {
            Name currentName = getCurrentName(personIdentityDetailed.getNames());
            mapName(currentName, personIdentity);
        }
        if (notNullAndNotEmpty(personIdentityDetailed.getBirthDates())) {
            personIdentity.setDateOfBirth(personIdentityDetailed.getBirthDates().get(0).getValue());
        }
        if (notNullAndNotEmpty(personIdentityDetailed.getAddresses())) {
            personIdentity.setAddresses(personIdentityDetailed.getAddresses());
        }
        return personIdentity;
    }

    PersonIdentityDetailed mapToPersonIdentityDetailed(PersonIdentityItem personIdentityItem) {
        List<Name> names = Collections.emptyList();
        if (notNullAndNotEmpty(personIdentityItem.getNames())) {
            names = mapPersonIdentityNames(personIdentityItem.getNames());
        }

        List<BirthDate> dobs = Collections.emptyList();
        if (notNullAndNotEmpty(personIdentityItem.getBirthDates())) {
            dobs = mapPersonIdentityBirthDates(personIdentityItem.getBirthDates());
        }

        List<Address> addresses = Collections.emptyList();
        if (notNullAndNotEmpty(personIdentityItem.getAddresses())) {
            addresses = mapCanonicalAddresses(personIdentityItem.getAddresses());
        }

        return new PersonIdentityDetailed(names, dobs, addresses, null);
    }

    private List<Address> mapCanonicalAddresses(List<CanonicalAddress> addresses) {
        return addresses.stream()
                .map(
                        address -> {
                            Address mappedAddress = new Address();
                            mappedAddress.setAddressCountry(address.getAddressCountry());
                            mappedAddress.setAddressLocality(address.getAddressLocality());
                            mappedAddress.setBuildingName(address.getBuildingName());
                            mappedAddress.setBuildingNumber(address.getBuildingNumber());
                            mappedAddress.setDepartmentName(address.getDepartmentName());
                            mappedAddress.setDependentAddressLocality(
                                    address.getDependentAddressLocality());
                            mappedAddress.setDependentStreetName(address.getDependentStreetName());
                            mappedAddress.setDoubleDependentAddressLocality(
                                    address.getDoubleDependentAddressLocality());
                            mappedAddress.setOrganisationName(address.getOrganisationName());
                            mappedAddress.setPostalCode(address.getPostalCode());
                            mappedAddress.setStreetName(address.getStreetName());
                            mappedAddress.setSubBuildingName(address.getSubBuildingName());
                            mappedAddress.setUprn(address.getUprn());
                            mappedAddress.setValidFrom(address.getValidFrom());
                            mappedAddress.setValidUntil(address.getValidUntil());
                            return mappedAddress;
                        })
                .collect(Collectors.toList());
    }

    private List<BirthDate> mapPersonIdentityBirthDates(
            List<PersonIdentityDateOfBirth> birthDates) {
        return birthDates.stream()
                .map(
                        birthDate -> {
                            BirthDate mappedBirthDate = new BirthDate();
                            mappedBirthDate.setValue(birthDate.getValue());
                            return mappedBirthDate;
                        })
                .collect(Collectors.toList());
    }

    private List<Name> mapPersonIdentityNames(List<PersonIdentityName> names) {
        return names.stream()
                .map(
                        name -> {
                            Name mappedName = new Name();
                            List<NamePart> mappedNameParts =
                                    name.getNameParts().stream()
                                            .map(
                                                    namePart -> {
                                                        NamePart mappedNamePart = new NamePart();
                                                        mappedNamePart.setType(namePart.getType());
                                                        mappedNamePart.setValue(
                                                                namePart.getValue());
                                                        return mappedNamePart;
                                                    })
                                            .collect(Collectors.toList());
                            mappedName.setNameParts(mappedNameParts);
                            return mappedName;
                        })
                .collect(Collectors.toList());
    }

    private <T> boolean notNullAndNotEmpty(List<T> items) {
        return Objects.nonNull(items) && !items.isEmpty();
    }

    private <T> T getCurrentName(List<T> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        throw new IllegalArgumentException("Unable to map person identity with multiple names");
    }

    private void mapName(Name name, PersonIdentity personIdentity) {
        List<NamePart> givenNameParts =
                name.getNameParts().stream()
                        .filter(
                                namePart ->
                                        namePart.getType()
                                                .equalsIgnoreCase(NamePartType.GIVEN_NAME.value))
                        .collect(Collectors.toList());
        List<NamePart> familyNameParts =
                name.getNameParts().stream()
                        .filter(
                                namePart ->
                                        namePart.getType()
                                                .equalsIgnoreCase(NamePartType.FAMILY_NAME.value))
                        .collect(Collectors.toList());

        personIdentity.setFirstName(givenNameParts.get(0).getValue());
        if (givenNameParts.size() > 1) {
            personIdentity.setMiddleNames(mapMiddleNames(givenNameParts, NamePart::getValue));
        }
        personIdentity.setSurname(familyNameParts.get(0).getValue());
    }

    private void mapName(PersonIdentityName name, PersonIdentity personIdentity) {
        List<PersonIdentityNamePart> givenNameParts =
                getNamePartsByType(name, NamePartType.GIVEN_NAME);
        List<PersonIdentityNamePart> familyNameParts =
                getNamePartsByType(name, NamePartType.FAMILY_NAME);

        if (givenNameParts.isEmpty()) {
            throw new IllegalArgumentException("No given names found. Cannot map firstname");
        }
        if (familyNameParts.isEmpty()) {
            throw new IllegalArgumentException("No family names found. Cannot map surname");
        } else if (familyNameParts.size() > 1) {
            throw new IllegalArgumentException("More than 1 family name found. Cannot map surname");
        }
        personIdentity.setFirstName(givenNameParts.get(0).getValue());
        if (givenNameParts.size() > 1) {
            personIdentity.setMiddleNames(
                    mapMiddleNames(givenNameParts, PersonIdentityNamePart::getValue));
        }
        personIdentity.setSurname(familyNameParts.get(0).getValue());
    }

    private <T> String mapMiddleNames(List<T> nameParts, Function<T, String> mappingFunction) {
        return String.join(
                " ",
                nameParts.subList(1, nameParts.size()).stream()
                        .map(mappingFunction)
                        .toArray(String[]::new));
    }

    private List<PersonIdentityNamePart> getNamePartsByType(
            PersonIdentityName name, NamePartType namePartType) {
        return name.getNameParts().stream()
                .filter(np -> np.getType().equals(namePartType.value))
                .collect(Collectors.toList());
    }

    private List<PersonIdentityDateOfBirth> mapBirthDates(List<BirthDate> birthDates) {
        return birthDates.stream()
                .map(
                        bd -> {
                            PersonIdentityDateOfBirth dob = new PersonIdentityDateOfBirth();
                            dob.setValue(bd.getValue());
                            return dob;
                        })
                .collect(Collectors.toList());
    }

    private List<PersonIdentityName> mapNames(List<Name> names) {
        return names.stream()
                .map(
                        n -> {
                            PersonIdentityName name = new PersonIdentityName();
                            if (notNullAndNotEmpty(n.getNameParts())) {
                                name.setNameParts(
                                        n.getNameParts().stream()
                                                .map(
                                                        np -> {
                                                            PersonIdentityNamePart namePart =
                                                                    new PersonIdentityNamePart();
                                                            namePart.setType(np.getType());
                                                            namePart.setValue(np.getValue());
                                                            return namePart;
                                                        })
                                                .collect(Collectors.toList()));
                            }
                            return name;
                        })
                .collect(Collectors.toList());
    }

    private List<CanonicalAddress> mapAddresses(List<Address> addresses) {
        return addresses.stream()
                .map(
                        a -> {
                            CanonicalAddress canonicalAddress = new CanonicalAddress();
                            canonicalAddress.setUprn(a.getUprn());
                            canonicalAddress.setOrganisationName(a.getOrganisationName());
                            canonicalAddress.setDepartmentName(a.getDepartmentName());
                            canonicalAddress.setSubBuildingName(a.getSubBuildingName());
                            canonicalAddress.setBuildingNumber(a.getBuildingNumber());
                            canonicalAddress.setBuildingName(a.getBuildingName());
                            canonicalAddress.setDependentStreetName(a.getDependentStreetName());
                            canonicalAddress.setStreetName(a.getStreetName());
                            canonicalAddress.setAddressCountry(a.getAddressCountry());
                            canonicalAddress.setPostalCode(a.getPostalCode());
                            if (Objects.nonNull(a.getValidFrom())) {
                                canonicalAddress.setValidFrom(a.getValidFrom());
                            }
                            if (Objects.nonNull(a.getValidUntil())) {
                                canonicalAddress.setValidUntil(a.getValidUntil());
                            }
                            canonicalAddress.setAddressLocality(a.getAddressLocality());
                            canonicalAddress.setDependentAddressLocality(
                                    a.getDependentAddressLocality());
                            canonicalAddress.setDoubleDependentAddressLocality(
                                    a.getDoubleDependentAddressLocality());

                            return canonicalAddress;
                        })
                .collect(Collectors.toList());
    }
}
