package uk.gov.di.ipv.cri.common.library.util.retry;

public interface Retryable<T> {
    T execute();
}
