package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import java.time.LocalDate;

public class PersonAddress {
    private String buildingNumber;
    private String buildingName;
    private String flat;
    private String street;
    private String townCity;
    private String postcode;
    private String district;
    private PersonAddressType addressType;
    private LocalDate dateMovedOut;
    private LocalDate dateMovedIn;

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getFlat() {
        return flat;
    }

    public void setFlat(String flat) {
        this.flat = flat;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getTownCity() {
        return townCity;
    }

    public void setTownCity(String townCity) {
        this.townCity = townCity;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public PersonAddressType getAddressType() {
        return addressType;
    }

    public void setAddressType(PersonAddressType addressType) {
        this.addressType = addressType;
    }

    public LocalDate getDateMovedOut() {
        return dateMovedOut;
    }

    public void setDateMovedOut(LocalDate dateMovedOut) {
        this.dateMovedOut = dateMovedOut;
    }

    public LocalDate getDateMovedIn() {
        return dateMovedIn;
    }

    public void setDateMovedIn(LocalDate dateMovedIn) {
        this.dateMovedIn = dateMovedIn;
    }
}
