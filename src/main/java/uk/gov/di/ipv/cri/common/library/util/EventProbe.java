package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import uk.gov.di.ipv.cri.common.library.error.GenericException;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventProbe {
    private static final String GOVUK_SIGNIN_JOURNEY_ID = "govuk_signin_journey_id";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final MetricsLogger METRICS_LOGGER = MetricsUtils.metricsLogger();
    private static final String JSON_REGEX = "\\{.*\\}";
    private static final Pattern PATTERN = Pattern.compile(JSON_REGEX);
    private static final List<String> SENSITIVE_INFORMATION = List.of(
            // Shared claims
            "name",
            "GivenName",
            "FamilyName",
            "birthDate",
            "address",
            "buildingNumber",
            "streetName",
            "postalCode",

            // Generic capture
            "dob",
            "name",
            "address",
            "birth",
            "age",
            "build"
    );
    private static final String JSON_REMOVED_MSG = "[JSON Omitted]";
    private static final String PARTIAL_EXCEPTION_MSG = "Partial error message (stripped due to containing sensitive information): ";

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
            String errorMessage = stripJSON(cause.getMessage(), JSON_REMOVED_MSG);
            int sensitiveInformationStartIdx = containsSensitiveInformation(errorMessage);
            if(sensitiveInformationStartIdx != -1) {
                errorMessage = String.format("%s %s", PARTIAL_EXCEPTION_MSG, errorMessage.substring(0, sensitiveInformationStartIdx));
            }
            LOGGER.error(errorMessage, new GenericException(errorMessage));
        }
    }

    public EventProbe counterMetric(String key) {
        METRICS_LOGGER.putMetric(key, 1d);
        return this;
    }

    public EventProbe counterMetric(String key, double value) {
        METRICS_LOGGER.putMetric(key, value);
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
            METRICS_LOGGER.putDimensions(dimensionSet);
        }
    }

    private String stripJSON(String input, String replacement) {
        String str = input;
        Matcher matcher = PATTERN.matcher(str);
        while(matcher.find()) {
            str = str.replace(matcher.group(), replacement);
        }
        return str;
    }

    private int containsSensitiveInformation(String input) {
        int index = -1;
        for(String s : SENSITIVE_INFORMATION) {
            if(input.toLowerCase().contains(s)) {
                index = input.toLowerCase().indexOf(s);
                return index;
            }
        }
        return index;
    }
}
