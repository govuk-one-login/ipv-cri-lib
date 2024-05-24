package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import uk.gov.di.ipv.cri.common.library.domain.SessionRequest;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.AuthorizationCodeExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionExpiredException;
import uk.gov.di.ipv.cri.common.library.exception.SessionNotFoundException;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    private static final String SESSION_ID = UUID.randomUUID().toString();
    private static Instant fixedInstant;
    private SessionService sessionService;

    @Mock private DataStore<SessionItem> mockDataStore;
    @Mock private ConfigurationService mockConfigurationService;
    @Captor private ArgumentCaptor<SessionItem> sessionItemArgumentCaptor;

    @BeforeAll
    static void beforeAll() {
        fixedInstant = Instant.now();
    }

    @BeforeEach
    void setUp() {
        Clock nowClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        sessionService = new SessionService(mockDataStore, mockConfigurationService, nowClock);
    }

    @Test
    void shouldCallCreateOnSessionDataStore() {
        final long sessionExpirationEpoch = 10L;
        when(mockConfigurationService.getSessionExpirationEpoch())
                .thenReturn(sessionExpirationEpoch);
        SessionRequest sessionRequest = mock(SessionRequest.class);

        when(sessionRequest.getClientId()).thenReturn("a client id");
        when(sessionRequest.getState()).thenReturn("state");
        when(sessionRequest.getRedirectUri())
                .thenReturn(URI.create("https://www.example.com/callback"));
        when(sessionRequest.getSubject()).thenReturn("a subject");
        when(sessionRequest.getPersistentSessionId()).thenReturn("a persistent session id");
        when(sessionRequest.getClientSessionId()).thenReturn("a client session id");
        when(sessionRequest.getClientIpAddress()).thenReturn("192.0.2.0");

        try (MockedStatic<LoggingUtils> loggingUtilsMockedStatic =
                Mockito.mockStatic(LoggingUtils.class)) {
            sessionService.saveSession(sessionRequest);
            verifyLoggingUtilsAppendKeys(loggingUtilsMockedStatic);
        }
        verify(mockDataStore).create(sessionItemArgumentCaptor.capture());
        SessionItem capturedValue = sessionItemArgumentCaptor.getValue();
        assertNotNull(capturedValue.getSessionId());
        assertThat(capturedValue.getExpiryDate(), equalTo(sessionExpirationEpoch));
        assertThat(capturedValue.getClientId(), equalTo("a client id"));
        assertThat(capturedValue.getState(), equalTo("state"));
        assertThat(capturedValue.getSubject(), equalTo("a subject"));
        assertThat(capturedValue.getPersistentSessionId(), equalTo("a persistent session id"));
        assertThat(capturedValue.getClientSessionId(), equalTo("a client session id"));
        assertThat(capturedValue.getClientIpAddress(), equalTo("192.0.2.0"));
        assertThat(
                capturedValue.getRedirectUri(),
                equalTo(URI.create("https://www.example.com/callback")));
        assertThat(capturedValue.getAttemptCount(), equalTo(0));
    }

    @Test
    void shouldGetSessionItemByAuthorizationCodeIndexSuccessfully()
            throws AuthorizationCodeExpiredException, SessionExpiredException,
                    SessionNotFoundException {

        String authCodeValue = UUID.randomUUID().toString();
        SessionItem item = new SessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAuthorizationCode(authCodeValue);
        item.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        item.setAuthorizationCodeExpiryDate(
                Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        setClientSessionIds(item);
        List<SessionItem> items = List.of(item);

        when(mockDataStore.getItemByIndex(SessionItem.AUTHORIZATION_CODE_INDEX, authCodeValue))
                .thenReturn(items);
        when(mockDataStore.getItem(item.getSessionId().toString())).thenReturn(item);

        try (MockedStatic<LoggingUtils> loggingUtilsMockedStatic =
                Mockito.mockStatic(LoggingUtils.class)) {
            SessionItem sessionItem = sessionService.getSessionByAuthorisationCode(authCodeValue);
            assertThat(item.getSessionId(), equalTo(sessionItem.getSessionId()));
            assertThat(item.getAuthorizationCode(), equalTo(sessionItem.getAuthorizationCode()));
            verifyLoggingUtilsAppendKeys(loggingUtilsMockedStatic);
        }
    }

    @Test
    void shouldGetSessionItemByTokenIndexSuccessfully()
            throws AccessTokenExpiredException, SessionExpiredException, SessionNotFoundException {
        AccessToken accessToken = new BearerAccessToken();
        String serialisedAccessToken = accessToken.toAuthorizationHeader();
        SessionItem item = new SessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAccessToken(serialisedAccessToken);
        item.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        item.setAccessTokenExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        setClientSessionIds(item);
        List<SessionItem> items = List.of(item);

        when(mockDataStore.getItemByIndex(SessionItem.ACCESS_TOKEN_INDEX, serialisedAccessToken))
                .thenReturn(items);
        when(mockDataStore.getItem(item.getSessionId().toString())).thenReturn(item);

        try (MockedStatic<LoggingUtils> loggingUtilsMockedStatic =
                Mockito.mockStatic(LoggingUtils.class)) {
            SessionItem sessionItem = sessionService.getSessionByAccessToken(accessToken);
            assertThat(item.getSessionId(), equalTo(sessionItem.getSessionId()));
            assertThat(item.getAccessToken(), equalTo(sessionItem.getAccessToken()));
            verifyLoggingUtilsAppendKeys(loggingUtilsMockedStatic);
        }
    }

    @Test
    void shouldThrowExceptionWhenSessionExpired() {
        SessionItem expiredSessionItem = new SessionItem();
        expiredSessionItem.setExpiryDate(fixedInstant.minus(1, ChronoUnit.HOURS).getEpochSecond());
        when(mockDataStore.getItem(SESSION_ID)).thenReturn(expiredSessionItem);

        assertThrows(
                SessionExpiredException.class, () -> sessionService.validateSessionId(SESSION_ID));
    }

    @Test
    void saveThrowsSessionNotFound() {
        when(mockDataStore.getItem(SESSION_ID)).thenReturn(null);
        assertThrows(
                SessionNotFoundException.class, () -> sessionService.validateSessionId(SESSION_ID));
    }

    @Test
    void shouldThrowAuthorizationCodeExpiredException() {
        String authorizationCode = UUID.randomUUID().toString();
        SessionItem item = new SessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAuthorizationCode(authorizationCode);
        item.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        item.setAuthorizationCodeExpiryDate(
                Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond());
        List<SessionItem> items = List.of(item);

        when(mockDataStore.getItemByIndex(SessionItem.AUTHORIZATION_CODE_INDEX, authorizationCode))
                .thenReturn(items);
        when(mockDataStore.getItem(item.getSessionId().toString())).thenReturn(item);

        assertThrows(
                AuthorizationCodeExpiredException.class,
                () -> sessionService.getSessionByAuthorisationCode(authorizationCode));
    }

    @Test
    void shouldThrowAccessTokenExpiredException() {
        AccessToken accessToken = new BearerAccessToken();
        String serialisedAccessToken = accessToken.toAuthorizationHeader();
        SessionItem item = new SessionItem();
        item.setSessionId(UUID.randomUUID());
        item.setAccessToken(serialisedAccessToken);
        item.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        item.setAccessTokenExpiryDate(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond());
        List<SessionItem> items = List.of(item);

        when(mockDataStore.getItemByIndex(SessionItem.ACCESS_TOKEN_INDEX, serialisedAccessToken))
                .thenReturn(items);
        when(mockDataStore.getItem(item.getSessionId().toString())).thenReturn(item);

        assertThrows(
                AccessTokenExpiredException.class,
                () -> sessionService.getSessionByAccessToken(accessToken));
    }

    @Test
    void shouldUpdateSession() {
        SessionItem sessionItem = new SessionItem();
        setClientSessionIds(sessionItem);
        try (MockedStatic<LoggingUtils> loggingUtilsMockedStatic =
                Mockito.mockStatic(LoggingUtils.class)) {
            sessionService.updateSession(sessionItem);
            verifyLoggingUtilsAppendKeys(loggingUtilsMockedStatic);
        }

        verify(mockDataStore).update(sessionItem);
    }

    @Test
    void shouldGetSessionItemBySessionId() {

        try (MockedStatic<LoggingUtils> loggingUtilsMockedStatic =
                Mockito.mockStatic(LoggingUtils.class)) {
            SessionItem sessionItem = mock(SessionItem.class);
            when(mockDataStore.getItem(SESSION_ID)).thenReturn(sessionItem);
            when(sessionItem.getClientSessionId()).thenReturn("a client session id");
            sessionService.getSession(SESSION_ID);

            verify(mockDataStore).getItem(SESSION_ID);
            loggingUtilsMockedStatic.verify(
                    () -> LoggingUtils.appendKey("govuk_signin_journey_id", "a client session id"),
                    times(1));
            loggingUtilsMockedStatic.verifyNoMoreInteractions();
        }
    }

    private void verifyLoggingUtilsAppendKeys(MockedStatic<LoggingUtils> loggingUtilsMockedStatic) {
        loggingUtilsMockedStatic.verify(
                () -> LoggingUtils.appendKey("govuk_signin_journey_id", "a client session id"),
                times(1));
        loggingUtilsMockedStatic.verifyNoMoreInteractions();
    }

    private void setClientSessionIds(SessionItem item) {
        item.setClientSessionId("a client session id");
        item.setPersistentSessionId("a persistent session id");
    }
}
