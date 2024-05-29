package uk.gov.di.ipv.cri.common.library.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ListUtil {
    private ListUtil() {}

    public static <T> T getOneItemOrThrowError(List<T> list) throws IllegalArgumentException {
        if (Objects.isNull(list) || list.isEmpty()) {
            throw new IllegalArgumentException("No items found");
        } else if (list.size() > 1) {
            throw new IllegalArgumentException("More than one item found");
        } else {
            return list.get(0);
        }
    }

    /**
     * Split a list into batches of specified size.
     *
     * @param list The list to split into batches
     * @param batchSize The size of each batch. The last sublist will have between 1 and {@code
     *     batchSize} items
     * @return A list of items split into batches
     * @throws IllegalArgumentException If the list is null or the batch size is less than 1
     */
    public static <T> List<List<T>> split(List<T> list, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than 0");
        }

        List<List<T>> batches = new ArrayList<>();

        for (int idx = 0; idx < list.size(); idx += batchSize) {
            batches.add(list.subList(idx, Math.min(list.size(), idx + batchSize)));
        }

        return batches;
    }

    /**
     * Create a new list comprised of elements in the {@code source} list except those in the {@code
     * except} when compared using a custom {@code mapper} function.
     *
     * @param source The source list to filter
     * @param except The list containing the elements to exclude
     * @param mapper A function mapping list elements to values used to determine element equality
     */
    public static <T, U extends Comparable<? super U>> List<T> except(
            List<T> source, List<T> except, Function<? super T, ? extends U> mapper) {
        return except(source, except, Comparator.comparing(mapper));
    }

    /**
     * Create a new list comprised of elements in the {@code source} list except those in the {@code
     * except} when compared using a custom {@link Comparator comparator} function.
     *
     * @see ListUtil#except(List, List, Function)
     */
    public static <T> List<T> except(List<T> source, List<T> except, Comparator<T> comparator) {
        return source.stream()
                .filter(element -> !contains(except, element, comparator))
                .collect(Collectors.toList());
    }

    /**
     * Modify the {@code source} list to remove the elements contained in the {@code except} list
     * using when compared using a custom {@code mapper} function.
     *
     * @param source The source list to filter
     * @param except The list containing the elements to exclude
     * @param mapper A function mapping list elements to values used to determine element equality
     */
    public static <T, U extends Comparable<? super U>> void exclude(
            List<T> source, List<T> except, Function<? super T, ? extends U> mapper) {
        exclude(source, except, Comparator.comparing(mapper));
    }

    /**
     * Modify the {@code source} list to remove the elements contained in the {@code except} list
     * using when compared using a custom {@link Comparator comparator} function.
     *
     * @see ListUtil#exclude(List, List, Function)
     */
    public static <T> void exclude(List<T> source, List<T> except, Comparator<T> comparator) {
        source.removeIf(element -> contains(except, element, comparator));
    }

    /**
     * Merge two lists without duplicating any elements. Elements from the {@code source} list are
     * replaced by elements from the {@code merge} list if they are equal when evaluated using the
     * {@code mapper} function.
     *
     * @param source The source list to be modified with the new elements
     * @param merge The list with the elements to be added to the source list
     * @param mapper A function mapping list elements to values used to determine element equality
     */
    public static <T, U extends Comparable<? super U>> void mergeDistinct(
            List<T> source, List<T> merge, Function<? super T, ? extends U> mapper) {
        mergeDistinct(source, merge, Comparator.comparing(mapper));
    }

    /**
     * Merge two lists without duplicating any elements. Elements from the {@code source} list are
     * replaced by elements from the {@code merge} list if they are equal when evaluated using the
     * {@code comparator} function.
     *
     * @param comparator A {@link Comparator comparator} function used to determine element equality
     * @see ListUtil#mergeDistinct(List, List, Function)
     */
    public static <T> void mergeDistinct(List<T> source, List<T> merge, Comparator<T> comparator) {
        exclude(source, merge, comparator);
        source.addAll(merge);
    }

    /**
     * Determine whether a list contains an element using a custom comparator. This allows to select
     * only certain fields or specify a custom function to determine equality between objects.
     *
     * @param list The source list to test for existence of the object
     * @param object Determine whether this object is an element of the list given the {@code
     *     comparator}
     * @param comparator The {@link Comparator comparator} function used to determine object
     *     equality
     * @return {@code true} if the object is determined to be contained in the {@code list}
     */
    public static <T> boolean contains(List<T> list, T object, Comparator<T> comparator) {
        return list.stream().anyMatch(element -> objectsEqual(element, object, comparator));
    }

    private static <T> boolean objectsEqual(T a, T b, Comparator<T> comparator) {
        return comparator.compare(a, b) == 0;
    }
}
