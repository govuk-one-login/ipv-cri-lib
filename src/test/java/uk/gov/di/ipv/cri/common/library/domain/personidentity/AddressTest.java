package uk.gov.di.ipv.cri.common.library.domain.personidentity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AddressTest {
    @Test
    void shouldGetPreviousAddressTypeWhenValidUntilIsPastDate() {
        Address testAddress = new Address();
        testAddress.setValidUntil(LocalDate.of(2013, 8, 9));

        assertEquals(AddressType.PREVIOUS, testAddress.getAddressType());
    }

    @Test
    void shouldGetPreviousAddressTypeWithValidUntilIsToday() {
        Address testAddress = new Address();
        testAddress.setValidUntil(LocalDate.now());

        assertEquals(AddressType.PREVIOUS, testAddress.getAddressType());
    }

    @Test
    void shouldReturnNullAddressTypeWhenValidUntilIsInFuture() {
        Address testAddress = new Address();
        testAddress.setValidUntil(LocalDate.now().plusYears(1));

        assertNull(testAddress.getAddressType());
    }

    @Test
    void shouldGetPreviousAddressTypeWithValidUntilAndValidFrom() {
        Address testAddress = new Address();
        testAddress.setValidFrom(LocalDate.of(2013, 7, 9));
        testAddress.setValidUntil(LocalDate.of(2013, 8, 9));

        assertEquals(AddressType.PREVIOUS, testAddress.getAddressType());
    }

    @Test
    void shouldGetCurrentAddressTypeWithValidFromInPast() {
        Address testAddress = new Address();
        testAddress.setValidFrom(LocalDate.of(2021, 9, 10));

        assertEquals(AddressType.CURRENT, testAddress.getAddressType());
    }

    @Test
    void shouldReturnCurrentAddressTypeWhenValidFromAndValidUntilAreNull() {
        Address testAddress = new Address();

        assertEquals(AddressType.CURRENT, testAddress.getAddressType());
    }

    @Test
    void shouldReturnCurrentAddressTypeWhenValidFromIsToday() {
        Address testAddress = new Address();
        testAddress.setValidFrom(LocalDate.now());

        assertEquals(AddressType.CURRENT, testAddress.getAddressType());
    }

    @Test
    void shouldReturnNullAddressTypeWhenValidFromIsInFuture() {
        Address testAddress = new Address();
        testAddress.setValidFrom(LocalDate.now().plusYears(1));

        assertNull(testAddress.getAddressType());
    }

    @Test
    void shouldReturnNullAddressTypeWhenValidFromAndValidUntilIsToday() {
        Address testAddress = new Address();
        testAddress.setValidFrom(LocalDate.now());
        testAddress.setValidUntil(LocalDate.now());

        assertNull(testAddress.getAddressType());
    }

    @Test
    void shouldReturnNullAddressTypeWhenValidFromAndValidUntilAreInFuture() {
        Address testAddress = new Address();
        testAddress.setValidFrom(LocalDate.now().plusYears(1));
        testAddress.setValidUntil(LocalDate.now().plusYears(2));

        assertNull(testAddress.getAddressType());
    }

    @Test
    void shouldReturnNullAddressTypeWhenValidFromAndValidUntilAreReversed() {
        Address testAddress = new Address();
        testAddress.setValidFrom(LocalDate.now().minusYears(1));
        testAddress.setValidUntil(LocalDate.now().minusYears(2));

        assertNull(testAddress.getAddressType());
    }
}
