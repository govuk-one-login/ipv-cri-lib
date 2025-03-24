package uk.gov.di.ipv.cri.common.library.util.retry;

import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryManagerTest {

    @Test
    void shouldRetryThreeTimes() {
        String output = "Hello, World!";

        RetryConfig config = new RetryConfig.Builder().maxAttempts(3).build();

        AtomicInteger attempts = new AtomicInteger();

        Retryable<String> retryable =
                () -> {
                    if (attempts.getAndIncrement() < 2) {
                        throw new RuntimeException();
                    }
                    return output;
                };

        String result = RetryManager.execute(config, retryable);
        assertEquals(output, result);
        assertEquals(3, attempts.get());
    }

    @Test
    void shouldAbortOnExpectedException() {
        RetryConfig config =
                new RetryConfig.Builder()
                        .maxAttempts(3)
                        .abortOn(AccessTokenExpiredException.class::isInstance)
                        .build();

        AtomicInteger attempts = new AtomicInteger();

        Retryable<String> retryable =
                () -> {
                    if (attempts.getAndIncrement() == 1) {
                        throw new AccessTokenExpiredException("dummy exception");
                    } else {
                        throw new RuntimeException();
                    }
                };

        assertThrows(
                AccessTokenExpiredException.class, () -> RetryManager.execute(config, retryable));
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldRetryExponentially() {
        String output = "Hello, World!";

        long duration = 1000;
        long expectedExponentiallyRetry = duration + (duration * 2) + (duration * 3);

        long start = System.currentTimeMillis();

        RetryConfig config =
                new RetryConfig.Builder()
                        .maxAttempts(3)
                        .delayBetweenAttempts(duration)
                        .exponentiallyRetry(true)
                        .build();

        AtomicInteger attempts = new AtomicInteger();

        Retryable<String> retryable =
                () -> {
                    if (attempts.getAndIncrement() < 2) {
                        throw new RuntimeException("Retry attempt: " + attempts.get());
                    }
                    return output;
                };

        String result = RetryManager.execute(config, retryable);

        assertEquals(output, result);
        assertEquals(3, attempts.get());
        assertTrue((System.currentTimeMillis() - start) >= expectedExponentiallyRetry);
    }
}
