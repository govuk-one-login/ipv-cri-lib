package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;
import uk.gov.di.ipv.cri.common.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.AuthorizationCodeExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.DynamoDbEnhancedClientFactory;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.util.ListUtil;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public class SessionService {
    private static final String SESSION_TABLE_PARAM_NAME = "SessionTableName";
    private static final String GOVUK_SIGNIN_JOURNEY_ID = "govuk_signin_journey_id";
    private final ConfigurationService configurationService;
    private final DataStore<SessionItem> dataStore;
    private final ListUtil listUtil;
    private final Clock clock;

    @ExcludeFromGeneratedCoverageReport
    public SessionService() {
        this.configurationService = new ConfigurationService();
        this.dataStore =
                new DataStore<>(
                        configurationService.getCommonParameterValue(SESSION_TABLE_PARAM_NAME),
                        SessionItem.class,
                        new DynamoDbEnhancedClientFactory().getClient());
        this.clock = Clock.systemUTC();
        this.listUtil = new ListUtil();
    }

    @ExcludeFromGeneratedCoverageReport
    public SessionService(ConfigurationService configurationService) {
        this(
                new DataStore<>(
                        configurationService.getCommonParameterValue(SESSION_TABLE_PARAM_NAME),
                        SessionItem.class,
                        new DynamoDbEnhancedClientFactory().getClient()),
                configurationService,
                Clock.systemUTC(),
                new ListUtil());
    }

    public SessionService(
            DataStore<SessionItem> dataStore,
            ConfigurationService configurationService,
            Clock clock,
            ListUtil listUtil) {
        this.dataStore = dataStore;
        this.configurationService = configurationService;
        this.clock = clock;
        this.listUtil = listUtil;
    }

    public UUID saveSession(SessionRequest sessionRequest) {
        SessionItem sessionItem = new SessionItem();
        sessionItem.setCreatedDate(clock.instant().getEpochSecond());
        sessionItem.setExpiryDate(configurationService.getSessionExpirationEpoch());
        sessionItem.setState(sessionRequest.getState());
        sessionItem.setClientId(sessionRequest.getClientId());
        sessionItem.setRedirectUri(sessionRequest.getRedirectUri());
        sessionItem.setSubject(sessionRequest.getSubject());
        sessionItem.setPersistentSessionId(sessionRequest.getPersistentSessionId());
        sessionItem.setClientSessionId(sessionRequest.getClientSessionId());
        sessionItem.setClientIpAddress(sessionRequest.getClientIpAddress());
        sessionItem.setAttemptCount(0);
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
                        s ->
                                Optional.ofNullable(s.getClientSessionId())
                                        .ifPresent(
                                                id ->
                                                        LoggingUtils.appendKey(
                                                                GOVUK_SIGNIN_JOURNEY_ID, id)));
    }

    public SessionItem getSession(String sessionId) {
        SessionItem sessionItem = dataStore.getItem(sessionId);
        setSessionItemsToLogging(sessionItem);
        return sessionItem;
    }

    public SessionItem getSessionByAccessToken(AccessToken accessToken)
            throws SessionExpiredException, AccessTokenExpiredException, SessionNotFoundException {
        SessionItem sessionItem;

        try {
            sessionItem =
                    listUtil.getOneItemOrThrowError(
                            dataStore.getItemByIndex(
                                    SessionItem.ACCESS_TOKEN_INDEX,
                                    accessToken.toAuthorizationHeader()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("No items found")) {
                throw new SessionNotFoundException("no session found with that access token");
            } else {
                throw new SessionNotFoundException(
                        "more than one session found with that access token");
            }
        }

        // Re-fetch our session directly to avoid problems with projections
        sessionItem = validateSessionId(String.valueOf(sessionItem.getSessionId()));
        if (sessionItem.getAccessTokenExpiryDate() < clock.instant().getEpochSecond()) {
            throw new AccessTokenExpiredException("access code expired");
        }

        return sessionItem;
    }

    public SessionItem getSessionByAuthorisationCode(String authCode)
            throws SessionExpiredException, AuthorizationCodeExpiredException,
                    SessionNotFoundException {
        SessionItem sessionItem;

        try {
            sessionItem =
                    listUtil.getOneItemOrThrowError(
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
