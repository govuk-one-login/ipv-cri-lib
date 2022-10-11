package uk.gov.di.ipv.cri.common.library.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.util.StringUtils;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

@ExcludeFromGeneratedCoverageReport
public class LogHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String COMPONENT_ID = "passport-cri";
    public static final String GOVUK_SIGNIN_JOURNEY_ID_DEFAULT_VALUE = "unknown";

    public enum LogField {
        CLIENT_ID_LOG_FIELD("clientId"),
        COMPONENT_ID_LOG_FIELD("componentId"),
        ERROR_CODE_LOG_FIELD("errorCode"),
        ERROR_DESCRIPTION_LOG_FIELD("errorDescription"),
        PASSPORT_SESSION_ID_LOG_FIELD("passportSessionId"),
        GOVUK_SIGNIN_JOURNEY_ID("govuk_signin_journey_id"),
        JTI_LOG_FIELD("jti"),
        USED_AT_DATE_TIME_LOG_FIELD("usedAtDateTime");

        private final String fieldName;

        LogField(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return this.fieldName;
        }
    }

    private LogHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static void attachComponentIdToLogs() {
        attachFieldToLogs(LogField.COMPONENT_ID_LOG_FIELD, COMPONENT_ID);
    }

    public static void attachClientIdToLogs(String clientId) {
        attachFieldToLogs(LogField.CLIENT_ID_LOG_FIELD, clientId);
    }

    public static void attachPassportSessionIdToLogs(String sessionId) {
        attachFieldToLogs(LogField.PASSPORT_SESSION_ID_LOG_FIELD, sessionId);
    }

    public static void attachGovukSigninJourneyIdToLogs(String govukSigninJourneyId) {
        if (StringUtils.isNullOrEmpty(govukSigninJourneyId)) {
            attachFieldToLogs(
                    LogField.GOVUK_SIGNIN_JOURNEY_ID, GOVUK_SIGNIN_JOURNEY_ID_DEFAULT_VALUE);
        } else {
            attachFieldToLogs(LogField.GOVUK_SIGNIN_JOURNEY_ID, govukSigninJourneyId);
        }
    }

    public static void logOauthError(String message, String errorCode, String errorDescription) {
        LoggingUtils.appendKey(LogField.ERROR_CODE_LOG_FIELD.getFieldName(), errorCode);
        LoggingUtils.appendKey(
                LogField.ERROR_DESCRIPTION_LOG_FIELD.getFieldName(), errorDescription);
        LOGGER.error(message);
        LoggingUtils.removeKeys(
                LogField.ERROR_CODE_LOG_FIELD.getFieldName(),
                LogField.ERROR_DESCRIPTION_LOG_FIELD.getFieldName());
    }

    private static void attachFieldToLogs(LogField field, String value) {
        LoggingUtils.appendKey(field.getFieldName(), value);
        LOGGER.info("{} attached to logs", field);
    }
}
