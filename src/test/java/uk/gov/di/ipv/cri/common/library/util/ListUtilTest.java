package uk.gov.di.ipv.cri.common.library.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

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

    @Test
    void split_shouldThrowIfListIsNull() {
        assertThrows(NullPointerException.class, () -> ListUtil.split(null, 1));
    }

    @Test
    void split_shouldThrowIfBatchSizeIsNegative() {
        final List<Object> list = Collections.emptyList();

        assertThrows(IllegalArgumentException.class, () -> ListUtil.split(list, -1));
    }

    @Test
    void split_shouldThrowIfBatchSizeIsZero() {
        final List<Object> list = Collections.emptyList();

        assertThrows(IllegalArgumentException.class, () -> ListUtil.split(list, 0));
    }

    @Test
    void split_shouldHandleSingletonList() {
        final List<Integer> list = Collections.singletonList(1);

        final List<List<Integer>> batches = ListUtil.split(list, 10);

        assertEquals(1, batches.size());
        assertEquals(list, batches.get(0));
    }

    @Test
    void split_shouldReturnSingleListIfBatchSizeIsGreaterThanListSize() {
        final List<Integer> list = List.of(1, 2, 3);

        final List<List<Integer>> batches = ListUtil.split(list, 10);

        assertEquals(1, batches.size());
        assertEquals(list, batches.get(0));
    }

    @Test
    void split_shouldSplitListIntoBatchesOfEqualSize() {
        final List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

        final List<List<Integer>> batches = ListUtil.split(list, 4);

        assertEquals(3, batches.size());
        assertEquals(List.of(1, 2, 3, 4), batches.get(0));
        assertEquals(List.of(5, 6, 7, 8), batches.get(1));
        assertEquals(List.of(9, 10, 11, 12), batches.get(2));
    }

    @Test
    void split_shouldPutRemainingElementsIntoTheLastBatch() {
        final List<Integer> list = List.of(1, 2, 3, 4, 5);

        final List<List<Integer>> batches = ListUtil.split(list, 3);

        assertEquals(2, batches.size());
        assertEquals(List.of(1, 2, 3), batches.get(0));
        assertEquals(List.of(4, 5), batches.get(1));
    }
}
