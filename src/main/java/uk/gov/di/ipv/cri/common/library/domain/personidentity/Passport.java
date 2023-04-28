package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Passport {
    @JsonProperty(value = "documentNumber")
    private String documentNumber;

    @JsonProperty(value = "icaoIssuerCode")
    private String icaoIssuerCode;

    @JsonProperty(value = "expiryDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String expiryDate;

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getIcaoIssuerCode() {
        return icaoIssuerCode;
    }

    public void setIcaoIssuerCode(String icaoIssuerCode) {
        this.icaoIssuerCode = icaoIssuerCode;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }
}
