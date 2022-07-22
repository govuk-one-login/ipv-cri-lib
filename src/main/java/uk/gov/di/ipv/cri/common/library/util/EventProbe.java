package uk.gov.di.ipv.cri.common.library.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MapMessage;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

import java.util.Map;
import java.util.Objects;

public class EventProbe {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final MetricsLogger metricsLogger = MetricsUtils.metricsLogger();

    public EventProbe log(Level level, Throwable throwable) {
        LOGGER.log(level, throwable.getMessage(), throwable);
        if (level == Level.ERROR) {
            logErrorCause(throwable);
        }
        return this;
    }

    public EventProbe log(Level level, MapMessage mapMessage) {
        LOGGER.log(level, mapMessage);
        return this;
    }

    private void logErrorCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (Objects.nonNull(cause)) {
            LOGGER.error(cause.getMessage(), cause);
            logErrorCause(cause);
        }
    }

    public EventProbe counterMetric(String key) {
        metricsLogger.putMetric(key, 1d);
        return this;
    }

    public EventProbe counterMetric(String key, double value) {
        metricsLogger.putMetric(key, value);
        return this;
    }

    public EventProbe auditEvent(Object event) {
        LOGGER.info(() -> "sending audit event " + event);
        return this;
    }

    public void addDimensions(Map<String, String> dimensions) {
        if (dimensions != null) {
            DimensionSet dimensionSet = new DimensionSet();
            dimensions.forEach(dimensionSet::addDimension);
            metricsLogger.putDimensions(dimensionSet);
        }
    }
}
