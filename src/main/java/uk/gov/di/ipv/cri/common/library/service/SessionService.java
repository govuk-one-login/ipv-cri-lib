package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.AuthorizationCodeExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.util.ListUtil;
import uk.gov.di.ipv.cri.common.library.util.retry.RetryConfig;
import uk.gov.di.ipv.cri.common.library.util.retry.RetryManager;
import uk.gov.di.ipv.cri.common.library.util.retry.Retryable;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public class SessionService {
    private static final String SESSION_TABLE_PARAM_NAME = "SessionTableName";
    private static final String GOVUK_SIGNIN_JOURNEY_ID = "govuk_signin_journey_id";
    private static final String REQUESTED_VERIFICATION_SCORE = "requested_verification_score";
    private final ConfigurationService configurationService;
    private final DataStore<SessionItem> dataStore;
    private final Clock clock;

    @ExcludeFromGeneratedCoverageReport
    public SessionService(
            ConfigurationService configurationService,
            DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this(
                new DataStore<>(
                        configurationService.getCommonParameterValue(SESSION_TABLE_PARAM_NAME),
                        SessionItem.class,
                        dynamoDbEnhancedClient),
                configurationService,
                Clock.systemUTC());
    }

    public SessionService(
            DataStore<SessionItem> dataStore,
            ConfigurationService configurationService,
            Clock clock) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.clock = clock;
    }

    public UUID saveSession(SessionRequest sessionRequest) {
        SessionItem sessionItem = new SessionItem();
        sessionItem.setCreatedDate(clock.instant().getEpochSecond());
        sessionItem.setExpiryDate(configurationService.getSessionExpirationEpoch());
        sessionItem.setState(sessionRequest.getState());
        sessionItem.setClientId(sessionRequest.getClientId());
        sessionItem.setEvidenceRequest(sessionRequest.getEvidenceRequest());
        sessionItem.setRedirectUri(sessionRequest.getRedirectUri());
        sessionItem.setSubject(sessionRequest.getSubject());
        sessionItem.setPersistentSessionId(sessionRequest.getPersistentSessionId());
        sessionItem.setClientSessionId(sessionRequest.getClientSessionId());
        sessionItem.setClientIpAddress(sessionRequest.getClientIpAddress());
        sessionItem.setAttemptCount(0);
        sessionItem.setContext(sessionRequest.getContext());
        setSessionItemsToLogging(sessionItem);

        dataStore.create(sessionItem);
        return sessionItem.getSessionId();
    }

    public void updateSession(SessionItem sessionItem) {
        setSessionItemsToLogging(sessionItem);
        dataStore.update(sessionItem);
    }

    public void createAuthorizationCode(SessionItem session) {
        session.setAuthorizationCode(UUID.randomUUID().toString());
        session.setAuthorizationCodeExpiryDate(
                configurationService.getAuthorizationCodeExpirationEpoch());
        updateSession(session);
    }

    public SessionItem validateSessionId(String sessionId)
            throws SessionNotFoundException, SessionExpiredException {

        if (sessionId == null || sessionId.isBlank()) {
            throw new SessionNotFoundException("session id empty");
        }

        SessionItem sessionItem = dataStore.getItem(sessionId);
        setSessionItemsToLogging(sessionItem);
        if (sessionItem == null) {
            throw new SessionNotFoundException("session not found");
        }

        if (sessionItem.getExpiryDate() < clock.instant().getEpochSecond()) {
            throw new SessionExpiredException("session expired");
        }

        return sessionItem;
    }

    private void setSessionItemsToLogging(SessionItem sessionItem) {
        Optional.ofNullable(sessionItem)
                .ifPresent(
                        s -> {
                            Optional.ofNullable(s.getClientSessionId())
                                    .ifPresent(
                                            id ->
                                                    LoggingUtils.appendKey(
                                                            GOVUK_SIGNIN_JOURNEY_ID, id));
                            Optional.ofNullable(s.getEvidenceRequest())
                                    .ifPresent(
                                            ev ->
                                                    LoggingUtils.appendKey(
                                                            REQUESTED_VERIFICATION_SCORE,
                                                            String.valueOf(
                                                                    ev.getVerificationScore())));
                        });
    }

    public SessionItem getSession(String sessionId) {
        SessionItem sessionItem = dataStore.getItem(sessionId);
        setSessionItemsToLogging(sessionItem);
        return sessionItem;
    }

    public SessionItem getSessionByAccessToken(AccessToken accessToken)
            throws SessionExpiredException, AccessTokenExpiredException, SessionNotFoundException {

        RetryConfig retryConfig =
                new RetryConfig.Builder()
                        .delayBetweenAttempts(500)
                        .maxAttempts(3)
                        .exponentiallyRetry(true)
                        .build();

        Retryable<SessionItem> retryable =
                () -> {
                    SessionItem sessionItem;

                    try {
                        sessionItem =
                                ListUtil.getOneItemOrThrowError(
                                        dataStore.getItemByIndex(
                                                SessionItem.ACCESS_TOKEN_INDEX,
                                                accessToken.toAuthorizationHeader()));
                    } catch (IllegalArgumentException e) {
                        if (e.getMessage().contains("No items found")) {
                            throw new SessionNotFoundException(
                                    "no session found with that access token");
                        } else {
                            throw new SessionNotFoundException(
                                    "more than one session found with that access token");
                        }
                    }

                    // Re-fetch our session directly to avoid problems with projections
                    sessionItem = validateSessionId(String.valueOf(sessionItem.getSessionId()));

                    return sessionItem;
                };

        SessionItem sessionItem = RetryManager.execute(retryConfig, retryable);

        if (sessionItem.getAccessTokenExpiryDate() < clock.instant().getEpochSecond()) {
            throw new AccessTokenExpiredException("access code expired");
        }

        return sessionItem;
    }

    public SessionItem getSessionByAuthorisationCode(String authCode)
            throws SessionExpiredException,
                    AuthorizationCodeExpiredException,
                    SessionNotFoundException {
        SessionItem sessionItem;

        try {
            sessionItem =
                    ListUtil.getOneItemOrThrowError(
                            dataStore.getItemByIndex(
                                    SessionItem.AUTHORIZATION_CODE_INDEX, authCode));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("No items found")) {
                throw new SessionNotFoundException("no session found with that authorization code");
            } else {
                throw new SessionNotFoundException(
                        "more than one session found with that authorization code");
            }
        }

        // Re-fetch our session directly to avoid problems with projections
        sessionItem = validateSessionId(String.valueOf(sessionItem.getSessionId()));

        if (sessionItem.getAuthorizationCodeExpiryDate() < clock.instant().getEpochSecond()) {
            throw new AuthorizationCodeExpiredException("authorization code expired");
        }

        return sessionItem;
    }
}
