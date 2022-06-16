package uk.gov.di.ipv.cri.common.library.util;

import java.util.List;

public class ListUtil {
    public <T> T getOneItemOrThrowError(List<T> list) throws IllegalArgumentException {
        if (list.size() == 0) {
            throw new IllegalArgumentException("No items found");
        } else if (list.size() > 1) {
            throw new IllegalArgumentException("More than one item found");
        } else {
            return list.get(0);
        }
    }
}
