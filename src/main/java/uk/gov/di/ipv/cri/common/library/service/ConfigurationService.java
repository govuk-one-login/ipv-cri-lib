package uk.gov.di.ipv.cri.common.library.service;

import software.amazon.lambda.powertools.parameters.secrets.SecretsProvider;
import software.amazon.lambda.powertools.parameters.ssm.SSMProvider;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ConfigurationService {
    private static final String MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT =
            "Environment variable %s is not set";
    private static final String PARAMETER_NAME_FORMAT = "/%s/%s";
    private static final long DEFAULT_BEARER_TOKEN_TTL_IN_SECS = 3600L;
    private static final long AUTHORIZATION_CODE_TTL_IN_SECS = 600L;
    private static final long DEFAULT_SESSION_TTL_IN_SECS = 7200L;
    private static final long DEFAULT_MAXIMUM_JWT_TTL = 6L;
    private final SSMProvider ssmProvider;
    private final SecretsProvider secretsProvider;
    private final String parameterPrefix;
    private final String commonParameterPrefix;
    private final String secretPrefix;
    private final Clock clock;

    @ExcludeFromGeneratedCoverageReport
    public ConfigurationService(SSMProvider ssmProvider, SecretsProvider secretsProvider) {
        this(
                ssmProvider,
                secretsProvider,
                System.getenv("AWS_STACK_NAME"),
                System.getenv("COMMON_PARAMETER_NAME_PREFIX"),
                Optional.ofNullable(System.getenv("SECRET_PREFIX"))
                        .orElse(System.getenv("AWS_STACK_NAME")),
                Clock.systemUTC());
    }

    ConfigurationService(
            SSMProvider ssmProvider,
            SecretsProvider secretsProvider,
            String parameterPrefix,
            String commonParameterPrefix,
            String secretPrefix,
            Clock clock) {
        this.ssmProvider = ssmProvider;
        this.secretsProvider = secretsProvider;
        this.parameterPrefix = parameterPrefix;
        this.commonParameterPrefix = commonParameterPrefix;
        this.secretPrefix = secretPrefix;
        this.clock = clock;
    }

    public String getParameterValue(String parameterName) {
        return ssmProvider.get(
                String.format(PARAMETER_NAME_FORMAT, parameterPrefix, parameterName));
    }

    public String getParameterValueByAbsoluteName(String parameterName) {
        return ssmProvider.get(parameterName);
    }

    public String getCommonParameterValue(String parameterName) {
        return ssmProvider.get(
                String.format(PARAMETER_NAME_FORMAT, getCommonParameterPrefix(), parameterName));
    }

    public String getSecretValue(String secretName) {
        return secretsProvider.get(String.format(PARAMETER_NAME_FORMAT, secretPrefix, secretName));
    }

    public Map<String, String> getParametersForPath(String path) {
        String format = String.format(PARAMETER_NAME_FORMAT, parameterPrefix, path);
        return ssmProvider.recursive().getMultiple(format.replace("//", "/"));
    }

    public long getSessionTtl() {
        return Optional.ofNullable(System.getenv("SESSION_TTL"))
                .map(Long::parseLong)
                .orElse(DEFAULT_SESSION_TTL_IN_SECS);
    }

    public long getSessionExpirationEpoch() {
        return clock.instant().plus(getSessionTtl(), ChronoUnit.SECONDS).getEpochSecond();
    }

    public long getAuthorizationCodeTtl() {
        return Optional.ofNullable(System.getenv("AUTHORIZATION_CODE_TTL"))
                .map(Long::parseLong)
                .orElse(AUTHORIZATION_CODE_TTL_IN_SECS);
    }

    public long getAuthorizationCodeExpirationEpoch() {
        return clock.instant().plus(getAuthorizationCodeTtl(), ChronoUnit.SECONDS).getEpochSecond();
    }

    public long getBearerAccessTokenTtl() {
        return Optional.ofNullable(System.getenv("BEARER_TOKEN_TTL"))
                .map(Long::parseLong)
                .orElse(DEFAULT_BEARER_TOKEN_TTL_IN_SECS);
    }

    public long getBearerAccessTokenExpirationEpoch() {
        return clock.instant().plus(getBearerAccessTokenTtl(), ChronoUnit.SECONDS).getEpochSecond();
    }

    public Long getMaxJwtTtl() {
        return Optional.ofNullable(System.getenv("MAXIMUM_JWT_TTL"))
                .map(Long::parseLong)
                .orElse(DEFAULT_MAXIMUM_JWT_TTL);
    }

    public String getVerifiableCredentialIssuer() {
        return getEnvironment("VERIFIABLE_CREDENTIAL_ISSUER");
    }

    public String getVerifiableCredentialKmsSigningKeyId() {
        return getEnvironment("VERIFIABLE_CREDENTIAL_SIGNING_KEY_ID");
    }

    public String getSqsAuditEventQueueUrl() {
        return getEnvironment("SQS_AUDIT_EVENT_QUEUE_URL");
    }

    public String getSqsAuditEventPrefix() {
        return getEnvironment("SQS_AUDIT_EVENT_PREFIX");
    }

    /**
     * @deprecated Use {@code session_decryption_key_active_alias} instead. This fallback key will
     *     be removed in future. See {@link KMSRSADecrypter}.
     */
    @SuppressWarnings("java:S1133")
    @Deprecated(forRemoval = true)
    public String getKmsEncryptionKeyId() {
        return getEnvironment("AUTH_REQUEST_KMS_ENCRYPTION_KEY_ID");
    }

    private String getCommonParameterPrefix() {
        return Objects.nonNull(commonParameterPrefix) ? commonParameterPrefix : parameterPrefix;
    }

    private String getEnvironment(String variable) {
        return Optional.ofNullable(System.getenv(variable))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format(
                                                MISSING_ENV_VARIABLE_ERROR_MSG_FORMAT, variable)));
    }
}
