package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsFactory;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import uk.gov.di.ipv.cri.common.library.helper.LogHelper;

import java.util.Map;
import java.util.Objects;

public class EventProbe {
    private static final Logger LOGGER = LogManager.getLogger(EventProbe.class);
    private final Metrics metrics;

    public EventProbe() {
        this(MetricsFactory.getMetricsInstance());
    }

    public EventProbe(Metrics metrics) {
        this.metrics = metrics;
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
        metrics.addMetric(key, 1d);
        return this;
    }

    public EventProbe counterMetric(String key, double value) {
        metrics.addMetric(key, value);
        return this;
    }

    public EventProbe counterMetric(String key, double value, MetricUnit unit) {
        metrics.addMetric(key, value, unit);
        return this;
    }

    public EventProbe auditEvent(Object event) {
        LOGGER.info(() -> "sending audit event " + event);
        return this;
    }

    public EventProbe addFieldToLoggingContext(String name, String value) {
        MDC.put(name, value);
        return this;
    }

    public EventProbe addJourneyIdToLoggingContext(String journeyId) {
        if (StringUtils.isNotBlank(journeyId)) {
            LogHelper.attachGovukSigninJourneyIdToLogs(journeyId);
        }
        return this;
    }

    public void addDimensions(Map<String, String> dimensions) {
        if (dimensions != null) {
            DimensionSet dimensionSet = new DimensionSet();
            dimensions.forEach(dimensionSet::addDimension);
            metrics.addDimension(dimensionSet);
        }
    }

    public static String clean(String metricValue) {
        return metricValue.replace(" ", "_").trim();
    }
}
