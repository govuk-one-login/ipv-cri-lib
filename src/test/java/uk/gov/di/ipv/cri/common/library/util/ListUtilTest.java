package uk.gov.di.ipv.cri.common.library.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ListUtilTest {

    // *** #getOneItemOrThrowError ***//

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

    // *** #split ***//

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
    void split_shouldReturnEmptyListIfSourceListIsEmpty() {
        final List<List<Object>> list = ListUtil.split(Collections.emptyList(), 10);
        assertEquals(0, list.size());
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

    // *** #except ***//

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
        final List<Point> exclude = Collections.singletonList(new Point(1, 4));

        assertEquals(List.of(new Point(3, 4)), ListUtil.except(points, exclude, Point::getX));
        assertEquals(List.of(new Point(1, 2)), ListUtil.except(points, exclude, Point::getY));
    }

    // *** #exclude ***//

    @Test
    void exclude_shouldThrowIfSourceListIsNull() {
        final List<Object> except = Collections.emptyList();

        assertThrows(
                NullPointerException.class, () -> ListUtil.exclude(null, except, Object::toString));
    }

    @Test
    void exclude_shouldThrowIfExcludeListIsNull() {
        final ArrayList<Integer> source = new ArrayList<>(List.of(1));

        assertThrows(
                NullPointerException.class, () -> ListUtil.exclude(source, null, Object::toString));
    }

    @Test
    void exclude_shouldMakeEmptyListIfSourceListIsEmpty() {
        final List<Object> list = Collections.emptyList();

        ListUtil.exclude(list, Collections.emptyList(), Object::toString);
        assertEquals(Collections.emptyList(), list);

        ListUtil.exclude(list, List.of(1, 2, 3), Object::toString);
        assertEquals(Collections.emptyList(), list);
    }

    @Test
    void exclude_shouldMakeEmptyListIfSourceListMatchesExcludeList() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));

        ListUtil.exclude(list, List.copyOf(list), Object::toString);
        assertEquals(Collections.emptyList(), list);
    }

    @Test
    void exclude_shouldReturnSourceListIfExcludeListIsEmpty() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));

        ListUtil.exclude(list, Collections.emptyList(), Object::toString);
        assertEquals(List.copyOf(list), list);
    }

    @Test
    void exclude_shouldReturnSourceListIfExcludeListDoesNotHaveCommonElements() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));

        ListUtil.exclude(list, List.of(4, 5, 6), Object::toString);
        assertEquals(List.copyOf(list), list);
    }

    @Test
    void exclude_shouldExcludeAllElementsFromExcludeList() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));

        ListUtil.exclude(list, List.of(2, 4, 5), Object::toString);
        assertEquals(List.of(1, 3, 6), list);
    }

    @Test
    void exclude_shouldExcludeAllElementsContainedInExcludeList() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));

        ListUtil.exclude(list, List.of(1, 3, 7, 8), Object::toString);
        assertEquals(List.of(2, 4, 5, 6), list);
    }

    @Test
    void exclude_shouldExcludeComplexTypes() {
        final List<Point> points = new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 4)));

        ListUtil.exclude(points, Collections.singletonList(points.get(1)), Object::toString);
        assertEquals(List.of(new Point(1, 2)), points);
    }

    @Test
    void exclude_shouldExcludeCollections() {
        final List<Collection<Integer>> lists =
                new ArrayList<>(List.of(Arrays.asList(1, 2, 3), List.of(4, 5, 6)));

        ListUtil.exclude(lists, Collections.singletonList(lists.get(1)), Object::toString);
        assertEquals(List.of(List.of(1, 2, 3)), lists);
    }

    @Test
    void exclude_shouldExcludeMaps() {
        final List<Map<String, Integer>> maps =
                new ArrayList<>(Arrays.asList(Map.of("one", 1), Map.of("two", 2)));

        ListUtil.exclude(maps, Collections.singletonList(maps.get(1)), Object::toString);
        assertEquals(List.of(Map.of("one", 1)), maps);
    }

    @Test
    void exclude_shouldExcludeUsingCustomComparator() {
        final List<Point> points = new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 4)));
        final List<Point> exclude = Collections.singletonList(new Point(1, 4));
        List<Point> list = new ArrayList<>(points);

        ListUtil.exclude(
                list, exclude, Comparator.comparing(Point::getY).thenComparing(Point::getX));

        assertEquals(points, list);

        ListUtil.exclude(list, exclude, Comparator.comparing(Point::getX));
        assertEquals(List.of(new Point(3, 4)), list);

        ListUtil.exclude(list, exclude, Comparator.comparing(Point::getY));
        assertEquals(Collections.emptyList(), list);
    }

    @Test
    void exclude_shouldExcludeUsingFieldValue() {
        final List<Point> points = new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 4)));
        final Point exclude = new Point(1, 4);

        ListUtil.exclude(points, Collections.singletonList(exclude), Point::getX);
        assertEquals(Collections.singletonList(new Point(3, 4)), points);

        ListUtil.exclude(points, Collections.singletonList(exclude), Point::getY);
        assertEquals(Collections.emptyList(), points);
    }

    // *** #mergeDistinct ***//

    @Test
    void mergeDistinct_shouldThrowIfSourceListIsNull() {
        final List<Object> merge = Collections.emptyList();

        assertThrows(
                NullPointerException.class,
                () -> ListUtil.mergeDistinct(null, merge, Object::toString));
    }

    @Test
    void mergeDistinct_shouldThrowIfMergeListIsNull() {
        final ArrayList<Integer> source = new ArrayList<>(List.of(1));

        assertThrows(
                NullPointerException.class,
                () -> ListUtil.mergeDistinct(source, null, Object::toString));
    }

    @Test
    void mergeDistinct_shouldAddToEmptySourceList() {
        final List<Object> list = new ArrayList<>();
        final List<Object> add = List.of(1, 2, 3);

        ListUtil.mergeDistinct(list, Collections.emptyList(), Object::toString);
        assertEquals(Collections.emptyList(), list);

        ListUtil.mergeDistinct(list, add, Object::toString);
        assertEquals(add, list);
    }

    @Test
    void mergeDistinct_shouldProduceSameListIfSourceListMatchesMergeList() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));

        ListUtil.mergeDistinct(list, List.copyOf(list), Object::toString);
        assertEquals(List.copyOf(list), list);
    }

    @Test
    void mergeDistinct_shouldProduceSameListIfMergeListIsEmpty() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));

        ListUtil.mergeDistinct(list, Collections.emptyList(), Object::toString);
        assertEquals(List.copyOf(list), list);
    }

    @Test
    void mergeDistinct_shouldAddTwoDistinctLists() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        final List<Integer> add = List.of(4, 5, 6);

        ListUtil.mergeDistinct(list, add, Object::toString);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), list);
    }

    @Test
    void mergeDistinct_shouldNotAddDuplicatedElements() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));

        ListUtil.mergeDistinct(list, List.of(2, 4, 5, 7, 8), Object::toString);
        assertEquals(List.of(1, 3, 6, 2, 4, 5, 7, 8), list);
    }

    @Test
    void mergeDistinct_shouldNotDuplicateComplexTypes() {
        final List<Point> points = new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 4)));

        ListUtil.mergeDistinct(points, Collections.singletonList(points.get(1)), Object::toString);
        assertEquals(List.copyOf(points), points);
    }

    @Test
    void mergeDistinct_shouldNotDuplicateCollections() {
        final List<Collection<Integer>> lists =
                new ArrayList<>(List.of(Arrays.asList(1, 2, 3), List.of(4, 5, 6)));

        ListUtil.mergeDistinct(lists, Collections.singletonList(lists.get(1)), Object::toString);
        assertEquals(List.copyOf(lists), lists);
    }

    @Test
    void mergeDistinct_shouldNotDuplicateMaps() {
        final List<Map<String, Integer>> maps =
                new ArrayList<>(Arrays.asList(Map.of("one", 1), Map.of("two", 2)));

        ListUtil.mergeDistinct(maps, Collections.singletonList(maps.get(1)), Object::toString);
        assertEquals(List.copyOf(maps), maps);
    }

    @Test
    void mergeDistinct_shouldAddDistinctComplexTypesUsingCustomComparator() {
        final List<Point> points = new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 5)));
        List<Point> list = new ArrayList<>(points);

        ListUtil.mergeDistinct(
                list,
                Collections.singletonList(new Point(1, 4)),
                Comparator.comparing(Point::getX));

        assertEquals(List.of(new Point(3, 5), new Point(1, 4)), list);

        ListUtil.mergeDistinct(
                list,
                Collections.singletonList(new Point(1, 5)),
                Comparator.comparing(Point::getY));

        assertEquals(List.of(new Point(1, 4), new Point(1, 5)), list);

        ListUtil.mergeDistinct(
                list,
                Collections.singletonList(new Point(1, 6)),
                Comparator.comparing(Point::getY).thenComparing(Point::getX));

        assertEquals(List.of(new Point(1, 4), new Point(1, 5), new Point(1, 6)), list);
    }

    @Test
    void mergeDistinct_shouldAddDistinctComplexTypesUsingFieldValue() {
        final List<Point> list = new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 4)));

        ListUtil.mergeDistinct(list, Collections.singletonList(new Point(2, 6)), Point::getX);
        assertEquals(List.of(new Point(1, 2), new Point(3, 4), new Point(2, 6)), list);

        ListUtil.mergeDistinct(list, Collections.singletonList(new Point(1, 5)), Point::getX);
        assertEquals(List.of(new Point(3, 4), new Point(2, 6), new Point(1, 5)), list);

        ListUtil.mergeDistinct(list, Collections.singletonList(new Point(1, 4)), Point::getY);
        assertEquals(List.of(new Point(2, 6), new Point(1, 5), new Point(1, 4)), list);
    }

    // *** #contains ***//

    @Test
    void contains_shouldThrowIfSourceListIsNull() {
        final Comparator<Integer> comparator = Comparator.naturalOrder();
        assertThrows(NullPointerException.class, () -> ListUtil.contains(null, 1, comparator));
    }

    @Test
    void contains_shouldThrowIfObjectIsNull() {
        final List<Integer> list = Collections.singletonList(1);
        final Comparator<Integer> comparator = Comparator.naturalOrder();

        assertThrows(NullPointerException.class, () -> ListUtil.contains(list, null, comparator));
    }

    @Test
    void contains_shouldReturnFalseIfSourceListIsEmpty() {
        assertFalse(ListUtil.contains(Collections.emptyList(), 1, Comparator.naturalOrder()));
    }

    @Test
    void contains_shouldReturnFalseIfListDoesNotContainPrimitiveObject() {
        assertFalse(ListUtil.contains(Collections.singletonList(1), 2, Comparator.naturalOrder()));
    }

    @Test
    void contains_shouldReturnTrueIfListContainsPrimitiveObject() {
        assertTrue(ListUtil.contains(List.of(1, 2, 3), 2, Comparator.naturalOrder()));
    }

    @Test
    void contains_shouldReturnTrueIfListContainsComplexObject() {
        assertTrue(
                ListUtil.contains(
                        List.of(new Point(1, 2), new Point(3, 4)),
                        new Point(1, 5),
                        Comparator.comparing(Point::getX)));
    }

    @Test
    void contains_shouldReturnFalseIfListDoesNotContainComplexObject() {
        assertFalse(
                ListUtil.contains(
                        List.of(new Point(1, 2), new Point(3, 4)),
                        new Point(1, 5),
                        Comparator.comparing(Point::getY)));
    }
}
