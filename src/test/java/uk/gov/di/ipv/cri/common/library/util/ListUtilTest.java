package uk.gov.di.ipv.cri.common.library.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ListUtilTest {

    @Test
    void getOneItemOrThrowError_shouldThrowIfListIsNull() {
        assertThrows(IllegalArgumentException.class, () -> ListUtil.getOneItemOrThrowError(null));
    }

    @Test
    void getOneItemOrThrowError_shouldThrowIfListIsEmpty() {
        final List<Object> list = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> ListUtil.getOneItemOrThrowError(list));
    }

    @Test
    void getOneItemOrThrowError_shouldThrowIfListHasMoreThanOneElement() {
        final List<Integer> list = List.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> ListUtil.getOneItemOrThrowError(list));
    }

    @Test
    void getOneItemOrThrowError_shouldReturnElementFromSingletonList() {
        assertEquals(1, ListUtil.getOneItemOrThrowError(Collections.singletonList(1)));
    }
}
