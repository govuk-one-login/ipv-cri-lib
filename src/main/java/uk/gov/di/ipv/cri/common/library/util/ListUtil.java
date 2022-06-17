package uk.gov.di.ipv.cri.common.library.util;

import java.util.List;
import java.util.Objects;

public class ListUtil {
    public <T> T getOneItemOrThrowError(List<T> list) throws IllegalArgumentException {
        if (Objects.isNull(list) || list.isEmpty()) {
            throw new IllegalArgumentException("No items found");
        } else if (list.size() > 1) {
            throw new IllegalArgumentException("More than one item found");
        } else {
            return list.get(0);
        }
    }
}
