package uk.gov.di.ipv.cri.common.library.util.retry;

import java.util.function.Predicate;

public class RetryConfig {
    private final int maxAttempts;
    private final long delayBetweenAttempts;
    private final boolean exponentiallyRetry;
    private final Predicate<Exception> abortCondition;

    private RetryConfig(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.delayBetweenAttempts = builder.delayBetweenAttempts;
        this.exponentiallyRetry = builder.exponentiallyRetry;
        this.abortCondition = builder.abortCondition;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getDelayBetweenAttempts() {
        return delayBetweenAttempts;
    }

    public boolean isExponentiallyRetry() {
        return exponentiallyRetry;
    }

    public Predicate<Exception> getAbortCondition() {
        return abortCondition;
    }

    public static class Builder {
        private int maxAttempts;
        private long delayBetweenAttempts;
        private boolean exponentiallyRetry;
        private Predicate<Exception> abortCondition;

        public Builder() {
            this.maxAttempts = 3;
            this.delayBetweenAttempts = 1000;
            this.exponentiallyRetry = false;
        }

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder delayBetweenAttempts(long delayBetweenAttemptsInMs) {
            this.delayBetweenAttempts = delayBetweenAttemptsInMs;
            return this;
        }

        public Builder exponentiallyRetry(boolean exponentiallyRetry) {
            this.exponentiallyRetry = exponentiallyRetry;
            return this;
        }

        public Builder abortOn(Predicate<Exception> condition) {
            this.abortCondition = condition;
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }
}
