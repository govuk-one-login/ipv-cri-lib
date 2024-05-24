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

    private static <T> boolean contains(List<T> list, T exclude, Comparator<T> comparator) {
        return list.stream().anyMatch(element -> objectsEqual(element, exclude, comparator));
    }

    private static <T> boolean objectsEqual(T a, T b, Comparator<T> comparator) {
        return comparator.compare(a, b) == 0;
    }
}
