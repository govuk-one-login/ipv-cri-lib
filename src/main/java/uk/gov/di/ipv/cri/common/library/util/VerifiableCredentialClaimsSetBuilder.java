package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.util.StringUtils;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class VerifiableCredentialClaimsSetBuilder {
    private final ConfigurationService configurationService;
    private final Clock clock;

    private Object verifiableCredentialSubject;
    private String verifiableCredentialType;
    private String[] contexts;
    private String subject;
    private Object evidence;
    private ChronoUnit ttlUnit;
    private long ttl;

    public VerifiableCredentialClaimsSetBuilder(
            ConfigurationService configurationService, Clock clock) {
        this.configurationService = configurationService;
        this.clock = clock;
    }

    public VerifiableCredentialClaimsSetBuilder subject(String subject) {
        if (StringUtils.isBlank(subject)) {
            throw new IllegalArgumentException("The subject must not be null or empty.");
        }
        this.subject = subject;
        return this;
    }

    public VerifiableCredentialClaimsSetBuilder timeToLive(long ttl, ChronoUnit ttlUnit) {
        this.ttlUnit = Objects.requireNonNull(ttlUnit, "ttlUnit must not be null");
        if (ttl < 1L) {
            throw new IllegalArgumentException("ttl must be greater than zero");
        }
        this.ttl = ttl;
        return this;
    }

    public VerifiableCredentialClaimsSetBuilder verifiableCredentialType(String type) {
        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException(
                    "The verifiable credential type must not be null or empty.");
        }
        this.verifiableCredentialType = type;
        return this;
    }

    public VerifiableCredentialClaimsSetBuilder verifiableCredentialSubject(Object subject) {
        this.verifiableCredentialSubject =
                Objects.requireNonNull(subject, "subject must not be null");
        return this;
    }

    public VerifiableCredentialClaimsSetBuilder verifiableCredentialContext(String[] contexts) {
        this.contexts = Objects.requireNonNull(contexts, "contexts must not be null");
        return this;
    }

    public VerifiableCredentialClaimsSetBuilder verifiableCredentialEvidence(Object evidence) {
        this.evidence = Objects.requireNonNull(evidence, "evidence must not be null");
        return this;
    }

    public JWTClaimsSet build() {
        if (StringUtils.isBlank(this.verifiableCredentialType)) {
            throw new IllegalStateException("The verifiable credential type must be specified");
        }
        if (StringUtils.isBlank(this.subject)) {
            throw new IllegalStateException("The subject must be specified");
        }
        if (Objects.isNull(this.verifiableCredentialSubject)) {
            throw new IllegalStateException("The verifiable credential subject must be specified");
        }
        String issuer = configurationService.getVerifiableCredentialIssuer();
        if (StringUtils.isBlank(issuer)) {
            throw new IllegalStateException(
                    "An empty/null verifiable credential issuer was retrieved from configuration");
        }
        if (this.ttl < 1L) {
            throw new IllegalStateException(
                    "An invalid verifiable credential time-to-live was encountered");
        }

        OffsetDateTime dateTimeNow = OffsetDateTime.now(this.clock);
        String vcExpiryRemovedReleaseFlag =
                configurationService.getParameterValueByAbsoluteName(
                        "/release-flags/vc-expiry-removed");
        boolean isVcExpiryRemovedReleaseFlag =
                Optional.ofNullable(vcExpiryRemovedReleaseFlag)
                        .map(x -> x.equalsIgnoreCase("true"))
                        .orElse(false);

        JWTClaimsSet.Builder builder =
                new JWTClaimsSet.Builder()
                        .subject(this.subject)
                        .issuer(issuer)
                        .claim(JWTClaimNames.NOT_BEFORE, dateTimeNow.toEpochSecond());

        if (!isVcExpiryRemovedReleaseFlag) {
            builder.claim(JWTClaimNames.EXPIRATION_TIME, getExpirationTimestamp(dateTimeNow));
        }

        Map<String, Object> verifiableCredentialClaims = new LinkedHashMap<>();
        verifiableCredentialClaims.put(
                "type", new String[] {"VerifiableCredential", this.verifiableCredentialType});
        if (Objects.nonNull(this.contexts) && contexts.length > 0) {
            verifiableCredentialClaims.put("@context", contexts);
        }
        verifiableCredentialClaims.put("credentialSubject", verifiableCredentialSubject);
        if (Objects.nonNull(evidence)) {
            verifiableCredentialClaims.put("evidence", evidence);
        }

        builder.claim("vc", verifiableCredentialClaims);

        return builder.build();
    }

    private long getExpirationTimestamp(OffsetDateTime dateTimeNow) {
        switch (this.ttlUnit) {
            case SECONDS:
                return dateTimeNow.plusSeconds(this.ttl).toEpochSecond();
            case MINUTES:
                return dateTimeNow.plusMinutes(this.ttl).toEpochSecond();
            case HOURS:
                return dateTimeNow.plusHours(this.ttl).toEpochSecond();
            case DAYS:
                return dateTimeNow.plusDays(this.ttl).toEpochSecond();
            case MONTHS:
                return dateTimeNow.plusMonths(this.ttl).toEpochSecond();
            case YEARS:
                return dateTimeNow.plusYears(this.ttl).toEpochSecond();
            default:
                throw new IllegalStateException(
                        "Unexpected time-to-live unit encountered: " + ttlUnit);
        }
    }
}
