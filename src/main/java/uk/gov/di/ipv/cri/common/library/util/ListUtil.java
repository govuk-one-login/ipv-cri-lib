package uk.gov.di.ipv.cri.common.library.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
}
