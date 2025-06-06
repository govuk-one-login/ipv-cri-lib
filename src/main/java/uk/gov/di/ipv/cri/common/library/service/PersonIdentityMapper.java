package uk.gov.di.ipv.cri.common.library.service;

import uk.gov.di.ipv.cri.common.library.domain.personidentity.Address;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PersonIdentityMapper {

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
        if (notNullAndNotEmpty(sharedClaims.getSocialSecurityRecords())) {
            identity.setSocialSecurityRecords(
                    mapSocialSecurityRecords(sharedClaims.getSocialSecurityRecords()));
        }
        if (notNullAndNotEmpty(sharedClaims.getDrivingPermits())) {
            identity.setDrivingPermits(
                    mapPersonIdentityDrivingPermits(sharedClaims.getDrivingPermits()));

            identity.setAddresses(
                    mapAddressesFromDrivingPermitFullAddress(sharedClaims.getDrivingPermits()));
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

        if (notNullAndNotEmpty(personIdentityItem.getSocialSecurityRecords())) {
            personIdentity.setSocialSecurityRecord(
                    mapSocialSecurityRecord(personIdentityItem.getSocialSecurityRecords()));
        }

        if (notNullAndNotEmpty(personIdentityItem.getDrivingPermits())) {
            personIdentity.setDrivingPermits(
                    mapDrivingPermits(personIdentityItem.getDrivingPermits()));
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
        if (notNullAndNotEmpty(personIdentityDetailed.getSocialSecurityRecords())) {
            personIdentity.setSocialSecurityRecord(
                    personIdentityDetailed.getSocialSecurityRecords());
        }
        return personIdentity;
    }

    @SuppressWarnings({"java:S1481", "java:S1854"})
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

        List<SocialSecurityRecord> socialSecurityRecords = Collections.emptyList();
        if (notNullAndNotEmpty(personIdentityItem.getSocialSecurityRecords())) {
            socialSecurityRecords =
                    mapPersonIdentitySocialSecurityRecords(
                            personIdentityItem.getSocialSecurityRecords());
        }

        List<DrivingPermit> drivingPermits = Collections.emptyList();
        if (notNullAndNotEmpty(personIdentityItem.getDrivingPermits())) {
            drivingPermits = mapDrivingPermits(personIdentityItem.getDrivingPermits());
            return PersonIdentityDetailedFactory.createPersonIdentityDetailedWithDrivingPermit(
                    names, dobs, addresses, drivingPermits);
        }

        return PersonIdentityDetailedFactory.createPersonIdentityDetailedWithAddresses(
                names, dobs, addresses);
    }

    private List<Address> mapCanonicalAddresses(List<CanonicalAddress> addresses) {
        return addresses.stream().map(Address::new).collect(Collectors.toList());
    }

    private List<SocialSecurityRecord> mapSocialSecurityRecord(
            List<PersonIdentitySocialSecurityRecord> socialSecurityRecords) {
        return socialSecurityRecords.stream()
                .map(SocialSecurityRecord::new)
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

    private List<SocialSecurityRecord> mapPersonIdentitySocialSecurityRecords(
            List<PersonIdentitySocialSecurityRecord> socialSecurityRecords) {
        return socialSecurityRecords.stream()
                .map(
                        securityRecord -> {
                            SocialSecurityRecord mappedSocialRecord = new SocialSecurityRecord();
                            mappedSocialRecord.setPersonalNumber(
                                    securityRecord.getPersonalNumber());
                            return mappedSocialRecord;
                        })
                .collect(Collectors.toList());
    }

    private List<PersonIdentityDrivingPermit> mapPersonIdentityDrivingPermits(
            List<DrivingPermit> drivingPermits) {
        return drivingPermits.stream()
                .map(
                        drivingPermit -> {
                            PersonIdentityDrivingPermit mappedDrivingPermit =
                                    new PersonIdentityDrivingPermit();
                            mappedDrivingPermit.setPersonalNumber(
                                    drivingPermit.getPersonalNumber());
                            mappedDrivingPermit.setExpiryDate(drivingPermit.getExpiryDate());
                            mappedDrivingPermit.setIssueDate(drivingPermit.getIssueDate());
                            mappedDrivingPermit.setIssueNumber(drivingPermit.getIssueNumber());
                            mappedDrivingPermit.setIssuedBy(drivingPermit.getIssuedBy());
                            mappedDrivingPermit.setFullAddress(drivingPermit.getFullAddress());
                            return mappedDrivingPermit;
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
                            canonicalAddress.setAddressRegion(a.getAddressRegion());
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

    private List<PersonIdentitySocialSecurityRecord> mapSocialSecurityRecords(
            List<SocialSecurityRecord> socialSecurityRecords) {
        return socialSecurityRecords.stream()
                .map(
                        sr -> {
                            PersonIdentitySocialSecurityRecord personalNumber =
                                    new PersonIdentitySocialSecurityRecord();
                            personalNumber.setPersonalNumber(sr.getPersonalNumber());
                            return personalNumber;
                        })
                .collect(Collectors.toList());
    }

    private List<DrivingPermit> mapDrivingPermits(
            List<PersonIdentityDrivingPermit> drivingPermits) {
        return drivingPermits.stream()
                .map(
                        dp -> {
                            DrivingPermit drivingPermit = new DrivingPermit();
                            drivingPermit.setPersonalNumber(dp.getPersonalNumber());
                            drivingPermit.setExpiryDate(dp.getExpiryDate());
                            drivingPermit.setIssueDate(dp.getIssueDate());
                            if (Objects.nonNull(dp.getIssueNumber())) {
                                drivingPermit.setIssueNumber(dp.getIssueNumber());
                            }
                            drivingPermit.setIssuedBy(dp.getIssuedBy());
                            drivingPermit.setFullAddress(dp.getFullAddress());

                            return drivingPermit;
                        })
                .collect(Collectors.toList());
    }

    private List<CanonicalAddress> mapAddressesFromDrivingPermitFullAddress(
            List<DrivingPermit> drivingPermits) {
        return drivingPermits.stream()
                .map(
                        dp -> {
                            if (Objects.nonNull(dp.getFullAddress())) {
                                CanonicalAddress canonicalAddress = new CanonicalAddress();

                                canonicalAddress.setPostalCode(
                                        extractPostalCodeFromDrivingPermitFullAddress(dp));

                                return canonicalAddress;
                            }

                            // Failed to extract postcode
                            return null;
                        })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String extractPostalCodeFromDrivingPermitFullAddress(DrivingPermit dp) {

        String postalCode = null;

        String fullAddress = dp.getFullAddress().toUpperCase();
        int len = fullAddress.length();

        if (fullAddress.length() >= 8) {
            fullAddress = fullAddress.substring(len - 8);

            // Remove Leading/Trailing Padding but not any separator space
            fullAddress = fullAddress.startsWith(",") ? fullAddress.substring(1) : fullAddress;
            fullAddress = fullAddress.stripLeading();
            fullAddress = fullAddress.stripTrailing();

            postalCode = fullAddress;
        } else if (fullAddress.length() == 7) {
            // Remove Leading/Trailing Padding but not any separator space
            fullAddress = fullAddress.startsWith(",") ? fullAddress.substring(1) : fullAddress;
            fullAddress = fullAddress.stripLeading();
            fullAddress = fullAddress.stripTrailing();

            postalCode = fullAddress;
        } else if (fullAddress.length() == 6) {
            postalCode = fullAddress;
        }

        return postalCode;
    }
}
