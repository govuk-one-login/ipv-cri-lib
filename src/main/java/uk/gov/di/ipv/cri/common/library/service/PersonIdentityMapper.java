package uk.gov.di.ipv.cri.common.library.service;

import uk.gov.di.ipv.cri.common.library.domain.CanonicalAddress;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonAddress;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonAddressType;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentity;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.Address;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.BirthDate;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.Name;
import uk.gov.di.ipv.cri.common.library.domain.sharedclaims.SharedClaims;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityDateOfBirth;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityItem;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityName;
import uk.gov.di.ipv.cri.common.library.persistence.item.personidentity.PersonIdentityNamePart;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
            mapAddresses(personIdentityItem.getAddresses(), personIdentity);
        }

        return personIdentity;
    }

    private <T> boolean notNullAndNotEmpty(List<T> items) {
        return Objects.nonNull(items) && !items.isEmpty();
    }

    private PersonIdentityName getCurrentName(List<PersonIdentityName> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        Optional<PersonIdentityName> currentName =
                names.stream()
                        .filter(
                                n ->
                                        Objects.nonNull(n.getValidFrom())
                                                && isPastDateOrToday(n.getValidFrom()))
                        .sorted(Comparator.comparing(PersonIdentityName::getValidFrom).reversed())
                        .findFirst();
        return currentName.orElseThrow(
                () -> new IllegalArgumentException("Unable to find current name. Cannot map name"));
    }

    private ChronoLocalDate getDateToday() {
        return ChronoLocalDate.from(ZonedDateTime.now());
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
                    String.join(
                            " ",
                            givenNameParts.subList(1, givenNameParts.size()).stream()
                                    .map(PersonIdentityNamePart::getValue)
                                    .toArray(String[]::new)));
        }
        personIdentity.setSurname(familyNameParts.get(0).getValue());
    }

    private List<PersonIdentityNamePart> getNamePartsByType(
            PersonIdentityName name, NamePartType namePartType) {
        return name.getNameParts().stream()
                .filter(np -> np.getType().equals(namePartType.value))
                .collect(Collectors.toList());
    }

    private void mapAddresses(
            List<CanonicalAddress> sourceAddresses, PersonIdentity personIdentity) {
        List<PersonAddress> personAddresses =
                sourceAddresses.stream()
                        .map(
                                sourceAddress -> {
                                    PersonAddress personAddress = new PersonAddress();
                                    personAddress.setBuildingNumber(
                                            sourceAddress.getBuildingNumber());
                                    personAddress.setBuildingName(sourceAddress.getBuildingName());
                                    personAddress.setStreet(sourceAddress.getStreetName());
                                    personAddress.setTownCity(sourceAddress.getAddressLocality());
                                    personAddress.setPostcode(sourceAddress.getPostalCode());
                                    personAddress.setAddressType(getAddressType(sourceAddress));
                                    personAddress.setDateMovedIn(sourceAddress.getValidFrom());
                                    personAddress.setDateMovedOut(sourceAddress.getValidUntil());
                                    return personAddress;
                                })
                        .collect(Collectors.toList());

        personIdentity.setAddresses(personAddresses);
    }

    private boolean isPastDate(LocalDate input) {
        return input.compareTo(getDateToday()) < 0;
    }

    private boolean isPastDateOrToday(LocalDate input) {
        return input.compareTo(getDateToday()) <= 0;
    }

    private PersonAddressType getAddressType(CanonicalAddress address) {
        if (Objects.nonNull(address.getValidUntil()) && isPastDate(address.getValidUntil())) {
            return PersonAddressType.PREVIOUS;
        }

        if (Objects.isNull(address.getValidUntil())
                && (Objects.nonNull(address.getValidFrom())
                        && isPastDateOrToday(address.getValidFrom()))) {
            return PersonAddressType.CURRENT;
        }

        return null;
    }

    private List<PersonIdentityDateOfBirth> mapBirthDates(List<BirthDate> birthDates) {
        return birthDates.stream()
                .map(
                        bd -> {
                            PersonIdentityDateOfBirth dob = new PersonIdentityDateOfBirth();
                            dob.setValue(LocalDate.parse(bd.getValue()));
                            return dob;
                        })
                .collect(Collectors.toList());
    }

    private List<PersonIdentityName> mapNames(List<Name> names) {
        return names.stream()
                .map(
                        n -> {
                            PersonIdentityName name = new PersonIdentityName();
                            name.setValidFrom(n.getValidFrom());
                            name.setValidUntil(n.getValidUntil());
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
                            canonicalAddress.setUprn(a.getUprn().orElse(null));
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
