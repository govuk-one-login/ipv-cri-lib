package uk.gov.di.ipv.cri.common.library.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.MetricsUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

public class EventProbe {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final MetricsLogger metricsLogger = MetricsUtils.metricsLogger();

    public EventProbe log(Level level, Exception e) {
        LOGGER.log(level, e);
        if (level == Level.ERROR) {
            if (Objects.nonNull(e.getCause())) {
                LOGGER.log(level, e.getCause());
                if (Objects.nonNull(e.getCause().getCause())) {
                    LOGGER.log(level, e.getCause().getCause());

                    LOGGER.error(e.getCause().getCause().getMessage(), e.getCause().getCause());
                    LOGGER.error(e.getCause().getCause().toString());
                }
                LOGGER.error(e.getCause().getMessage(), e.getCause());
                LOGGER.error(e.getCause().toString());
            }

            LOGGER.error(e.getMessage(), e);
            LOGGER.error(e.toString());

            try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
                e.printStackTrace(pw);
                String stacktrace = sw.toString();
                LOGGER.error(stacktrace);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        return this;
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
