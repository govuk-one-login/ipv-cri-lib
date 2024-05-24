package uk.gov.di.ipv.cri.common.library.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    @Test
    void except_shouldThrowIfSourceListIsNull() {
        final List<Object> except = Collections.emptyList();

        assertThrows(
                NullPointerException.class, () -> ListUtil.except(null, except, Object::toString));
    }

    @Test
    void except_shouldThrowIfExcludeListIsNull() {
        final List<Integer> source = Collections.singletonList(1);

        assertThrows(
                NullPointerException.class, () -> ListUtil.except(source, null, Object::toString));
    }

    @Test
    void except_shouldReturnEmptyListIfSourceListIsEmpty() {
        assertEquals(
                Collections.emptyList(),
                ListUtil.except(
                        Collections.emptyList(), Collections.emptyList(), Object::toString));

        assertEquals(
                Collections.emptyList(),
                ListUtil.except(Collections.emptyList(), List.of(1, 2, 3), Object::toString));
    }

    @Test
    void except_shouldReturnEmptyListIfSourceListMatchesExcludeList() {
        final List<Integer> list = List.of(1, 2, 3);
        assertEquals(
                Collections.emptyList(),
                ListUtil.except(list, List.copyOf(list), Object::toString));
    }

    @Test
    void except_shouldReturnSourceListIfExcludeListIsEmpty() {
        final List<Integer> list = List.of(1, 2, 3);
        assertEquals(list, ListUtil.except(list, Collections.emptyList(), Object::toString));
    }

    @Test
    void except_shouldReturnSourceListIfExcludeListDoesNotHaveCommonElements() {
        final List<Integer> list = List.of(1, 2, 3);
        assertEquals(list, ListUtil.except(list, List.of(4, 5, 6), Object::toString));
    }

    @Test
    void except_shouldExcludeAllElementsFromExcludeList() {
        final List<Integer> list = List.of(1, 2, 3, 4, 5, 6);
        assertEquals(List.of(1, 3, 6), ListUtil.except(list, List.of(2, 4, 5), Object::toString));
    }

    @Test
    void except_shouldExcludeAllElementsContainedInExcludeList() {
        final List<Integer> list = List.of(1, 2, 3, 4, 5, 6);
        assertEquals(
                List.of(2, 4, 5, 6), ListUtil.except(list, List.of(1, 3, 7, 8), Object::toString));
    }

    @Test
    void except_shouldExcludeComplexTypes() {
        final List<Point> points = List.of(new Point(1, 2), new Point(3, 4));

        assertEquals(
                List.of(new Point(1, 2)),
                ListUtil.except(
                        points, Collections.singletonList(points.get(1)), Object::toString));
    }

    @Test
    void except_shouldExcludeCollections() {
        final List<Collection<Integer>> lists = List.of(List.of(1, 2, 3), List.of(4, 5, 6));

        assertEquals(
                List.of(List.of(1, 2, 3)),
                ListUtil.except(lists, Collections.singletonList(lists.get(1)), Object::toString));
    }

    @Test
    void except_shouldExcludeMaps() {
        final List<Map<String, Integer>> maps = List.of(Map.of("one", 1), Map.of("two", 2));

        assertEquals(
                List.of(Map.of("one", 1)),
                ListUtil.except(maps, Collections.singletonList(maps.get(1)), Object::toString));
    }

    @Test
    void except_shouldExcludeUsingCustomComparator() {
        final List<Point> points = List.of(new Point(1, 2), new Point(3, 4));
        final List<Point> exclude = Collections.singletonList(new Point(1, 4));

        assertEquals(
                List.of(new Point(3, 4)),
                ListUtil.except(points, exclude, Comparator.comparing(Point::getX)));

        assertEquals(
                List.of(new Point(1, 2)),
                ListUtil.except(points, exclude, Comparator.comparing(Point::getY)));

        assertEquals(
                points,
                ListUtil.except(
                        points,
                        exclude,
                        Comparator.comparing(Point::getY).thenComparing(Point::getX)));
    }

    @Test
    void except_shouldExcludeUsingFieldValue() {
        final List<Point> points = List.of(new Point(1, 2), new Point(3, 4));
        final Point exclude = new Point(1, 4);

        assertEquals(
                List.of(new Point(3, 4)),
                ListUtil.except(points, Collections.singletonList(exclude), Point::getX));

        assertEquals(
                List.of(new Point(1, 2)),
                ListUtil.except(points, Collections.singletonList(exclude), Point::getY));
    }
}
