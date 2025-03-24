package uk.gov.di.ipv.cri.common.library.util.retry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.gov.di.ipv.cri.common.library.exception.RetryException;

public class RetryManager {
    private static final Logger LOGGER = LogManager.getLogger(RetryManager.class);

    private RetryManager() {
        throw new IllegalStateException("Static class");
    }

    public static <T> T execute(RetryConfig retryConfig, Retryable<T> retryable) {
        for (int attempt = 0; attempt < retryConfig.getMaxAttempts(); attempt++) {
            try {
                if (attempt > 0) {
                    long start = System.currentTimeMillis();
                    long sleepDuration = calculateSleepDuration(retryConfig, attempt);
                    LOGGER.info(
                            "Sleeping for {}ms at {}", sleepDuration, System.currentTimeMillis());
                    Thread.sleep(sleepDuration); // NOSONAR
                    long elapsed = System.currentTimeMillis() - start;
                    LOGGER.info("Slept for {}ms", elapsed);
                }

                T result = retryable.execute();

                if (attempt > 0) {
                    LOGGER.info("Retry succeeded on attempt {}", attempt);
                }

                return result;

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RetryException(ex);
            } catch (Exception e) {
                LOGGER.error("Retry attempt {} failed", attempt, e);
                if (shouldAbortRetry(retryConfig, e, attempt)) {
                    throw e;
                }
            }
        }

        throw new RetryException();
    }

    private static boolean shouldAbortRetry(RetryConfig retryConfig, Exception e, int attempt) {
        return attempt == retryConfig.getMaxAttempts() - 1
                || (retryConfig.getAbortCondition() != null
                        && retryConfig.getAbortCondition().test(e));
    }

    private static long calculateSleepDuration(RetryConfig retryConfig, int attempt) {
        if (attempt == 0) {
            return 0;
        }
        long duration = retryConfig.getDelayBetweenAttempts();
        if (retryConfig.isExponentiallyRetry()) {
            duration = (long) (duration * Math.pow(2, attempt));
        }
        return duration;
    }
}
