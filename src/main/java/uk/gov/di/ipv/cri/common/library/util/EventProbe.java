package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

import java.util.Map;
import java.util.Objects;

public class EventProbe {
    private static final String GOVUK_SIGNIN_JOURNEY_ID = "govuk_signin_journey_id";
    private static final Logger LOGGER = LogManager.getLogger();
    private final MetricsLogger metricsLogger;

    public EventProbe() {
        this(MetricsUtils.metricsLogger());
    }

    public EventProbe(MetricsLogger metricsLogger) {
        this.metricsLogger = metricsLogger;
    }

    public EventProbe log(Level level, Throwable throwable) {
        LOGGER.log(level, throwable.getMessage(), throwable);
        if (level == Level.ERROR) {
            logErrorCause(throwable);
        }
        return this;
    }

    public EventProbe log(Level level, String message) {
        if (LOGGER.isEnabled(level)) {
            LOGGER.log(level, message);
        }
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

    public EventProbe counterMetric(String key, double value, Unit unit) {
        metricsLogger.putMetric(key, value, unit);
        return this;
    }

    public EventProbe auditEvent(Object event) {
        LOGGER.info(() -> "sending audit event " + event);
        return this;
    }

    public EventProbe addFieldToLoggingContext(String name, String value) {
        LoggingUtils.appendKey(name, value);
        return this;
    }

    public EventProbe addJourneyIdToLoggingContext(String journeyId) {
        if (StringUtils.isNotBlank(journeyId)) {
            addFieldToLoggingContext(GOVUK_SIGNIN_JOURNEY_ID, journeyId);
        }
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
