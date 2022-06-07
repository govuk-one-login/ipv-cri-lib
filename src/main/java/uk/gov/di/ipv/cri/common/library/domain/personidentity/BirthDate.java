package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class BirthDate {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate value;

    public LocalDate getValue() {
        return value;
    }

    public void setValue(LocalDate value) {
        this.value = value;
    }
}
