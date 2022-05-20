package uk.gov.di.ipv.cri.common.library.persistence.item;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DynamoDbBean
public class CanonicalAddress {
    private Long uprn;
    private String organisationName;
    private String departmentName;
    private String subBuildingName;
    private String buildingNumber;
    private String buildingName;

    private String dependentThoroughfare;

    private String dependentStreetName;

    private String streetName;

    private String doubleDependentAddressLocality;

    private String dependentAddressLocality;

    private String addressLocality;

    private String postalCode;

    private String addressCountry;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date validFrom;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date validUntil;

    public CanonicalAddress() {
        // Default constructor
    }

    public Optional<Long> getUprn() {
        return Optional.ofNullable(this.uprn);
    }

    public void setUprn(Long uprn) {
        this.uprn = uprn;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getSubBuildingName() {
        return subBuildingName;
    }

    public void setSubBuildingName(String subBuildingName) {
        this.subBuildingName = subBuildingName;
    }

    public String getBuildingNumber() {
        return buildingNumber;
    }

    public void setBuildingNumber(String buildingNumber) {
        this.buildingNumber = buildingNumber;
    }

    public String getDependentStreetName() {
        return dependentStreetName;
    }

    public void setDependentStreetName(String dependentStreetName) {
        this.dependentStreetName = dependentStreetName;
    }

    public String getDoubleDependentAddressLocality() {
        return doubleDependentAddressLocality;
    }

    public void setDoubleDependentAddressLocality(String doubleDependentAddressLocality) {
        this.doubleDependentAddressLocality = doubleDependentAddressLocality;
    }

    public String getDependentAddressLocality() {
        return dependentAddressLocality;
    }

    public void setDependentAddressLocality(String dependentAddressLocality) {
        this.dependentAddressLocality = dependentAddressLocality;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getAddressLocality() {
        return addressLocality;
    }

    public void setAddressLocality(String addressLocality) {
        this.addressLocality = addressLocality;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public LocalDate getValidFrom() {
        return (Objects.nonNull(validFrom))
                ? new java.sql.Date(validFrom.getTime()).toLocalDate()
                : null;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = Objects.nonNull(validFrom) ? java.sql.Date.valueOf(validFrom) : null;
    }

    public LocalDate getValidUntil() {
        return (Objects.nonNull(validUntil))
                ? new java.sql.Date(validUntil.getTime()).toLocalDate()
                : null;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = Objects.nonNull(validUntil) ? java.sql.Date.valueOf(validUntil) : null;
    }
}
