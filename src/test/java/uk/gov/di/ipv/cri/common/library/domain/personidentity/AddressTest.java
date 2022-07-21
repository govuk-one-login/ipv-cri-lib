package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressTest {
    @Test
    void shouldGetPastAddressType() {
        Address testAddress = new Address();
        testAddress.setValidUntil(LocalDate.of(2013, 8, 9));

        assertEquals(AddressType.PREVIOUS, testAddress.getAddressType());
    }

    @Test
    void shouldGetCurrentAddressType() {
        Address testAddress = new Address();
        testAddress.setValidFrom(LocalDate.of(2021, 9, 10));

        assertEquals(AddressType.CURRENT, testAddress.getAddressType());
    }

    @Test
    void shouldReturnPreviousAddressTypeWhenValidFromAndValidUntilNotPresent() {
        Address testAddress = new Address();

        assertEquals(AddressType.PREVIOUS, testAddress.getAddressType());
    }
}
