package uk.gov.di.ipv.cri.common.library.service;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.JWTAuthentication;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.exception.AccessTokenValidationException;
import uk.gov.di.ipv.cri.common.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.common.library.exception.SessionValidationException;
import uk.gov.di.ipv.cri.common.library.persistence.DataStore;
import uk.gov.di.ipv.cri.common.library.persistence.item.AccessTokenItem;
import uk.gov.di.ipv.cri.common.library.persistence.item.SessionItem;
import uk.gov.di.ipv.cri.common.library.validation.ValidationResult;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {
    private static final String TEST_TABLE_NAME = "test-auth-code-table";
    @Mock private DataStore<AccessTokenItem> mockDataStore;
    @Mock private ConfigurationService mockConfigurationService;
    @Mock private JWTVerifier mockJwtVerifier;
    @InjectMocks private AccessTokenService accessTokenService;

    private final String SAMPLE_JWT =
            "eyJraWQiOiJpcHYtY29yZS1zdHViIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJpcHYtY29yZS1zdHViIiwiYXVkIjoiaHR0cHM6XC9cL2Rldi5hZGRyZXNzLmNyaS5hY2NvdW50Lmdvdi51ayIsIm5iZiI6MTY1MDU0MTg0MCwic2hhcmVkX2NsYWltcyI6eyJhZGRyZXNzZXMiOlt7InN0cmVldDEiOiI4Iiwic3RyZWV0MiI6IkhBRExFWSBST0FEIiwidG93bkNpdHkiOiJCQVRIIiwiY3VycmVudEFkZHJlc3MiOnRydWUsInBvc3RDb2RlIjoiQkEyIDVBQSJ9XSwibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IktFTk5FVEgiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IkRFQ0VSUVVFSVJBIiwidHlwZSI6IkZhbWlseU5hbWUifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjQtMDktMTkifV0sIkBjb250ZXh0IjpbImh0dHBzOlwvXC93d3cudzMub3JnXC8yMDE4XC9jcmVkZW50aWFsc1wvdjEiLCJodHRwczpcL1wvdm9jYWIubG9uZG9uLmNsb3VkYXBwcy5kaWdpdGFsXC9jb250ZXh0c1wvaWRlbnRpdHktdjEuanNvbmxkIl19LCJpc3MiOiJpcHYtY29yZS1zdHViIiwicmVkaXJlY3RfdXJpIjoiaHR0cHM6XC9cL2RpLWlwdi1jb3JlLXN0dWIubG9uZG9uLmNsb3VkYXBwcy5kaWdpdGFsXC9jYWxsYmFjayIsImV4cCI6MTY1MDU0NTQ0MCwiaWF0IjoxNjUwNTQxODQwLCJqdGkiOiJmNzM0ZTZjZi0xODVhLTQ3N2YtYjQxMi02YWU5ZTc0ODk5NzUifQ.lhizSFXqbQaBXwpnuanI4Ze69B4MSSoqfZLiDDVA7EEwuJSMx9ooB8zFUJORo7SWX-L-qGtM6vjGNhM7GGOLKxZhOZbES7UQu3D7ES5CpNiyZOAUVXnGDEISINF1bYJupS3ujbPfIkOMMoWdWxBpcVzh1TELpzqiYGAeMlSZUmZnIf5i8juysJi8C_DUKklnlF-iGUsCKjXfdNkDz4sx5VYnQu1rDckPUSsK0XKVcxu9lU7cqx39iNuqmkLgsgK1RvG6f1xIOJPUGm2HBfjzM8ZeV3zYlYU5Xa1umlfVptVPrcxMZEm6Iy-cH7d_1XqO1yXFTEzUdDlGL6UlKK7B1T2nAjBCP9YPhh59JQOohu2RnC6gz-kVHisJEPzYp3mAthLJ2KzeYk1BEDRbZo7jWQzYaVXoNgG_gCfDtep5aTKudDtkPtIWFJ3ENEvC2sItXNEFcKQrKkBBcvSmRy8DJE9A3mpPOTp6GaaNrONwbfRvjgcDSDew0i4_mw6Rg-GA0k10nQ874KRjpowzouTJvNCI1CYALIghUD-xNkC7N4TA0zHNiq2eeSI089LdVIsSz_tsGg4YZOKk7HVqmnm81lkeXBfIsUGkH3weI6f4kXZOFQr6YCu5bDqDXgzmSf0ocxprwf1b-OhzWGRmKluSJRMs2hU2Q8-AIVtG5NxrCGE";

    private final String JWT_MISSING_JTI =
            "eyJraWQiOiJpcHYtY29yZS1zdHViIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJpcHYtY29yZS1zdHViIiwiYXVkIjoiaHR0cHM6XC9cL2Rldi5hZGRyZXNzLmNyaS5hY2NvdW50Lmdvdi51ayIsIm5iZiI6MTY1MDU0MDkyNSwic2hhcmVkX2NsYWltcyI6eyJAY29udGV4dCI6WyJodHRwczpcL1wvd3d3LnczLm9yZ1wvMjAxOFwvY3JlZGVudGlhbHNcL3YxIiwiaHR0cHM6XC9cL3ZvY2FiLmxvbmRvbi5jbG91ZGFwcHMuZGlnaXRhbFwvY29udGV4dHNcL2lkZW50aXR5LXYxLmpzb25sZCJdLCJhZGRyZXNzZXMiOlt7InN0cmVldDEiOiI4Iiwic3RyZWV0MiI6IkhBRExFWSBST0FEIiwidG93bkNpdHkiOiJCQVRIIiwiY3VycmVudEFkZHJlc3MiOnRydWUsInBvc3RDb2RlIjoiQkEyIDVBQSJ9XSwibmFtZSI6W3sibmFtZVBhcnRzIjpbeyJ2YWx1ZSI6IktFTk5FVEgiLCJ0eXBlIjoiR2l2ZW5OYW1lIn0seyJ2YWx1ZSI6IkRFQ0VSUVVFSVJBIiwidHlwZSI6IkZhbWlseU5hbWUifV19XSwiYmlydGhEYXRlIjpbeyJ2YWx1ZSI6IjE5NjQtMDktMTkifV19LCJpc3MiOiJpcHYtY29yZS1zdHViIiwicmVkaXJlY3RfdXJpIjoiaHR0cHM6XC9cL2RpLWlwdi1jb3JlLXN0dWIubG9uZG9uLmNsb3VkYXBwcy5kaWdpdGFsXC9jYWxsYmFjayIsImV4cCI6MTY1MDU0NDUyNSwiaWF0IjoxNjUwNTQwOTI1fQ.qbT49i9CPImPMXj7_U_W5IKmqlyAMidXWcVajMxEsFmPvQCbfkGDJYUun2dnKeyUpkTNXdxBRgTjrl0ZyODxnaIrW4ZZD3dzm-9EoMoFFHKtttmYiucyVM65ZnCaDDu3IUVQulZ-5ADX8bn-pghIqd95NDE_oM8HDlGExcdtZuwOK-fPI4txABGPbgGV6it3HoXaeZr1JyLzJHunTM6mnYOvi50GULh0VPGDsOgNC5Mf61JPkzBvHJbnS9WcKzFIpl7zyfbyDJ9WWl5G88fBdErSjFdI5R0-gc3Cy3m3QYm76dwDfFZax7inbKnK1yyC8cBb8mvr3f5M9s6Mmckd9KFBymYid8M0acTbQi5XPBxOmIr0zeJZ85YQxtyvKswpASoWT6ap-VmglfBQ6MQ0Ql6VydLyYOuo4ZFLNX3uOD4TDEf-TCVKLO2sL3-GEQ4gZP59lHXQr4LD8aGnp_ikWLXBDk2toGcfXcUfA6Ph-67rKWjtDYYqanh4fqM-3dUmUVBkbq0341dHl_Y5igdvkxu7Gbj9X64sdurHE_ALnBTUHyMnjWLfbu_WmYM3qq4CHVrjNw-TgpQZxHHxhHJkUPmVn_gsoaVyb2TPAvecQ0iDbXhzXVR3Jw0tlhZgDtfz-8zEZyae5g6DRMsd6mWMhCx8LFWcsJtbm4_OCQ_Y6zU";

    @BeforeEach
    void setUp() {
        this.accessTokenService =
                new AccessTokenService(mockDataStore, mockConfigurationService, mockJwtVerifier);
    }

    @Test
    void shouldThrowExceptionForMissingJTI()
            throws AccessTokenValidationException, SessionValidationException,
                    ClientConfigurationException {

        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "ipv-core-stub";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=%s"
                                + "&grant_type=%s",
                        authCodeValue,
                        JWT_MISSING_JTI,
                        JWTAuthentication.CLIENT_ASSERTION_TYPE,
                        clientID,
                        grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(UUID.randomUUID());
        sessionItem.setAuthorizationCode(authCodeValue);
        sessionItem.setClientId(clientID);
        sessionItem.setRedirectUri(URI.create("https://www.example/com/callback"));
        when(mockConfigurationService.getParametersForPath(
                        "/clients/" + clientID + "/jwtAuthentication"))
                .thenReturn(getSSMConfigMap());
        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.validateTokenRequest(tokenRequest, sessionItem));

        assertThat(exception.getMessage(), containsString("jti is missing"));
        verify(mockJwtVerifier, never()).verifyAccessTokenJWT(any(), any(), any());
    }

    @Test
    void shouldCallCreateTokenRequestSuccessfully() throws AccessTokenValidationException {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=ipv-core-stub"
                                + "&grant_type=%s",
                        authCodeValue,
                        SAMPLE_JWT,
                        JWTAuthentication.CLIENT_ASSERTION_TYPE,
                        grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        AuthorizationCodeGrant authorizationCodeGrant =
                (AuthorizationCodeGrant) tokenRequest.getAuthorizationGrant();
        assertThat(tokenRequest, notNullValue());
        assertThat(
                authorizationCodeGrant.getAuthorizationCode().getValue(), equalTo(authCodeValue));
    }

    @Test
    void shouldThrowInvalidRequestWhenTokenRequestUrlParamsIsInComplete() {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&client_id=ipv-core-stub"
                                + "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, grantType);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString(OAuth2Error.INVALID_REQUEST.getCode()));
    }

    @Test
    void shouldThrowExceptionWhenNoMatchingAddressSessionItemForTheRequestedAuthorizationCode()
            throws AccessTokenValidationException {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "ipv-core-stub";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=%s"
                                + "&grant_type=%s",
                        authCodeValue,
                        SAMPLE_JWT,
                        JWTAuthentication.CLIENT_ASSERTION_TYPE,
                        clientID,
                        grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);

        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(UUID.randomUUID());
        sessionItem.setAuthorizationCode(authCodeValue);
        sessionItem.setClientId("123456789");
        sessionItem.setRedirectUri(URI.create("https://www.example/com/callback"));
        when(mockConfigurationService.getParametersForPath(
                        "/clients/" + clientID + "/jwtAuthentication"))
                .thenReturn(getSSMConfigMap());
        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.validateTokenRequest(tokenRequest, sessionItem));

        assertThat(
                exception.getMessage(),
                containsString("request client id and saved client id do not match"));
    }

    @Test
    void shouldThrowUnSupportGrantTypeExceptionWhenAuthorizationGrantTypeIsInValid() {
        String grantType = "not_authorization_code";
        String tokenRequestBody =
                String.format(
                        "code=some-code"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=ipv-core-stub"
                                + "&grant_type=%s",
                        SAMPLE_JWT, JWTAuthentication.CLIENT_ASSERTION_TYPE, grantType);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(exception.getMessage(), containsString(OAuth2Error.UNSUPPORTED_GRANT_TYPE_CODE));
    }

    @Test
    void shouldThrowInValidGrantExceptionWhenRedirectUriDoesNotMatchTheRetrievedItemRedirectUri()
            throws AccessTokenValidationException {
        String authCodeValue = "12345";
        String redirectUri = "http://test.com";
        String grantType = "authorization_code";
        String clientID = "ipv-core-stub";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=%s"
                                + "&client_assertion_type=%s"
                                + "&client_id=%s"
                                + "&grant_type=%s",
                        authCodeValue,
                        SAMPLE_JWT,
                        redirectUri,
                        JWTAuthentication.CLIENT_ASSERTION_TYPE,
                        clientID,
                        grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        SessionItem mockSessionItem = mock(SessionItem.class);

        when(mockSessionItem.getAuthorizationCode()).thenReturn(authCodeValue);
        when(mockSessionItem.getRedirectUri())
                .thenReturn(URI.create("http://different-redirectUri"));

        when(mockConfigurationService.getParametersForPath(
                        "/clients/" + clientID + "/jwtAuthentication"))
                .thenReturn(getSSMConfigMap());

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () ->
                                accessTokenService.validateTokenRequest(
                                        tokenRequest, mockSessionItem));

        assertThat(
                exception.getMessage(),
                containsString(
                        "redirect uri http://different-redirectUri does not match configuration uri https://www.example/com/callback"));
    }

    @Test
    void shouldCallWriteTokenAndUpdateDataStore() {
        var fixedInstant = Instant.parse("2020-01-01T00:00:00.00Z");

        AccessTokenResponse accessTokenResponse = mock(AccessTokenResponse.class);
        SessionItem sessionItem = new SessionItem();
        sessionItem.setAuthorizationCode("12345");

        Tokens mockTokens = mock(Tokens.class);
        when(accessTokenResponse.getTokens()).thenReturn(mockTokens);
        BearerAccessToken mockBearerAccessToken = mock(BearerAccessToken.class);
        when(mockTokens.getBearerAccessToken()).thenReturn(mockBearerAccessToken);
        when(mockBearerAccessToken.toAuthorizationHeader()).thenReturn("some-authorization-header");
        accessTokenService.updateSessionAccessToken(sessionItem, accessTokenResponse);

        assertThat(
                sessionItem.getAccessToken(),
                equalTo(
                        accessTokenResponse
                                .getTokens()
                                .getBearerAccessToken()
                                .toAuthorizationHeader()));

        assertNull(sessionItem.getAuthorizationCode());

        assertThat(
                sessionItem.getAccessTokenExpiryDate(),
                equalTo(mockConfigurationService.getBearerAccessTokenExpirationEpoch()));

        assertThat(accessTokenResponse, notNullValue());
    }

    @Test
    void shouldCallCreateTokenWithATokenRequest() {
        TokenRequest tokenRequest = mock(TokenRequest.class);
        TokenResponse tokenResponse = accessTokenService.createToken(tokenRequest);

        verify(tokenRequest).getScope();
        assertThat(tokenResponse, notNullValue());
    }

    private Map<String, String> getSSMConfigMap() {
        try {

            HashMap<String, String> map = new HashMap<>();
            map.put("redirectUri", "https://www.example/com/callback");
            map.put("authenticationAlg", "RS256");
            map.put("issuer", "ipv-core");
            return map;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void shouldValidateTokenRequestSuccessfully()
            throws AccessTokenValidationException, SessionValidationException,
                    ClientConfigurationException {

        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "ipv-core-stub";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=%s"
                                + "&grant_type=%s",
                        authCodeValue,
                        SAMPLE_JWT,
                        JWTAuthentication.CLIENT_ASSERTION_TYPE,
                        clientID,
                        grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);

        ClientAuthentication clientAuthentication = tokenRequest.getClientAuthentication();
        PrivateKeyJWT privateKeyJWT = (PrivateKeyJWT) clientAuthentication;
        SignedJWT signedJWT = privateKeyJWT.getClientAssertion();
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(UUID.randomUUID());
        sessionItem.setAuthorizationCode(authCodeValue);
        sessionItem.setClientId(clientID);
        sessionItem.setRedirectUri(URI.create("https://www.example/com/callback"));

        when(mockConfigurationService.getParametersForPath(
                        "/clients/" + clientID + "/jwtAuthentication"))
                .thenReturn(getSSMConfigMap());

        TokenRequest expectedTokenRequest =
                accessTokenService.validateTokenRequest(tokenRequest, sessionItem);

        assertEquals(expectedTokenRequest.getClientID(), tokenRequest.getClientID());
        verify(mockJwtVerifier, times(1))
                .verifyAccessTokenJWT(getSSMConfigMap(), signedJWT, new ClientID(clientID));
    }

    @Test
    void shouldThrowExceptionForMissingClientConfiguration()
            throws AccessTokenValidationException, SessionValidationException,
                    ClientConfigurationException {

        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "ipv-core-stub";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=%s"
                                + "&grant_type=%s",
                        authCodeValue,
                        SAMPLE_JWT,
                        JWTAuthentication.CLIENT_ASSERTION_TYPE,
                        clientID,
                        grantType);

        TokenRequest tokenRequest = accessTokenService.createTokenRequest(tokenRequestBody);
        SessionItem sessionItem = new SessionItem();
        sessionItem.setSessionId(UUID.randomUUID());
        sessionItem.setAuthorizationCode(authCodeValue);
        sessionItem.setClientId(clientID);

        when(mockConfigurationService.getParametersForPath(
                        "/clients/" + clientID + "/jwtAuthentication"))
                .thenReturn(Map.of());

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.validateTokenRequest(tokenRequest, sessionItem));

        assertThat(
                exception.getMessage(),
                containsString("no configuration for client id '" + clientID + "'"));
        verify(mockJwtVerifier, never()).verifyAccessTokenJWT(any(), any(), any());
    }

    @Test
    void shouldThrowParseExceptionWhenWrongAlgUsedForPrivateKeyJWT()
            throws SessionValidationException, ClientConfigurationException {

        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String clientID = "wrong-client-id";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=%s"
                                + "&grant_type=%s",
                        authCodeValue,
                        SAMPLE_JWT,
                        JWTAuthentication.CLIENT_ASSERTION_TYPE,
                        clientID,
                        grantType);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Invalid private key JWT authentication: The client identifier doesn't match the client assertion subject / issuer"));
        verify(mockJwtVerifier, never()).verifyAccessTokenJWT(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenClientAssertionTypeIsTheWrongValue()
            throws SessionValidationException, ClientConfigurationException {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String wrongClientAssertionType = "urn:ietf:params:oauth:wrong:jwt-bearer";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=ipv-core-stub"
                                + "&grant_type=%s",
                        authCodeValue, SAMPLE_JWT, wrongClientAssertionType, grantType);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Invalid client_assertion_type parameter, must be urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
        verify(mockJwtVerifier, never()).verifyAccessTokenJWT(any(), any(), any());
    }

    @Test
    void shouldThrowExceptionWhenExpectedJwtIsMalformed()
            throws SessionValidationException, ClientConfigurationException {
        String authCodeValue = "12345";
        String grantType = "authorization_code";
        String malformedJWT = "a-jwt-doesnt-look-like-this";
        String tokenRequestBody =
                String.format(
                        "code=%s"
                                + "&client_assertion=%s"
                                + "&redirect_uri=https://www.example/com/callback"
                                + "&client_assertion_type=%s"
                                + "&client_id=ipv-core-stub"
                                + "&grant_type=%s",
                        authCodeValue,
                        malformedJWT,
                        JWTAuthentication.CLIENT_ASSERTION_TYPE,
                        grantType);

        AccessTokenValidationException exception =
                assertThrows(
                        AccessTokenValidationException.class,
                        () -> accessTokenService.createTokenRequest(tokenRequestBody));

        assertThat(
                exception.getMessage(),
                containsString(
                        "Invalid client_assertion JWT: Invalid serialized unsecured/JWS/JWE object: Missing part delimiters"));
        verify(mockJwtVerifier, never()).verifyAccessTokenJWT(any(), any(), any());
    }

    @Test
    void shouldReturnSuccessfulTokenResponseOnSuccessfulExchange() {
        long testTokenTtl = 2400L;
        when(mockConfigurationService.getAccessTokenExpirySeconds()).thenReturn(testTokenTtl);

        TokenResponse response = accessTokenService.createToken();

        assertInstanceOf(AccessTokenResponse.class, response);
        assertNotNull(response.toSuccessResponse().getTokens().getAccessToken().getValue());
        assertEquals(
                testTokenTtl,
                response.toSuccessResponse().getTokens().getBearerAccessToken().getLifetime());
        assertEquals(
                AccessTokenService.DEFAULT_SCOPE,
                response.toSuccessResponse().getTokens().getBearerAccessToken().getScope());
    }

    @Test
    void shouldReturnValidationErrorWhenInvalidGrantTypeProvided() {
        ValidationResult<ErrorObject> validationResult =
                accessTokenService.validateAuthorizationGrant(
                        new RefreshTokenGrant(new RefreshToken()));

        assertNotNull(validationResult);
        assertFalse(validationResult.isValid());
        assertEquals(OAuth2Error.UNSUPPORTED_GRANT_TYPE, validationResult.getError());
    }

    @Test
    void shouldPersistAccessToken() {
        String testResourceId = UUID.randomUUID().toString();
        String testPassportSessionId = UUID.randomUUID().toString();
        AccessToken accessToken = new BearerAccessToken(3600L, null);
        AccessTokenResponse accessTokenResponse =
                new AccessTokenResponse(new Tokens(accessToken, null));
        ArgumentCaptor<AccessTokenItem> accessTokenItemArgCaptor =
                ArgumentCaptor.forClass(AccessTokenItem.class);

        accessTokenService.persistAccessToken(
                accessTokenResponse, testResourceId, testPassportSessionId);

        verify(mockDataStore).create(accessTokenItemArgCaptor.capture());
        AccessTokenItem capturedAccessTokenItem = accessTokenItemArgCaptor.getValue();
        assertNotNull(capturedAccessTokenItem);
        assertEquals(testResourceId, capturedAccessTokenItem.getResourceId());
        assertEquals(testPassportSessionId, capturedAccessTokenItem.getPassportSessionId());
        assertEquals(
                DigestUtils.sha256Hex(
                        accessTokenResponse.getTokens().getBearerAccessToken().getValue()),
                capturedAccessTokenItem.getAccessToken());
        assertEquals(testResourceId, capturedAccessTokenItem.getResourceId());
        assertNotNull(capturedAccessTokenItem.getAccessTokenExpiryDateTime());
    }

    @Test
    void shouldPersistAccessTokenWhenResourceIdNull() {
        String testPassportSessionId = UUID.randomUUID().toString();
        AccessToken accessToken = new BearerAccessToken(3600L, null);
        AccessTokenResponse accessTokenResponse =
                new AccessTokenResponse(new Tokens(accessToken, null));
        ArgumentCaptor<AccessTokenItem> accessTokenItemArgCaptor =
                ArgumentCaptor.forClass(AccessTokenItem.class);

        accessTokenService.persistAccessToken(accessTokenResponse, null, testPassportSessionId);

        verify(mockDataStore).create(accessTokenItemArgCaptor.capture());
        AccessTokenItem capturedAccessTokenItem = accessTokenItemArgCaptor.getValue();
        assertNotNull(capturedAccessTokenItem);
        assertNull(capturedAccessTokenItem.getResourceId());
        assertEquals(testPassportSessionId, capturedAccessTokenItem.getPassportSessionId());
        assertEquals(
                DigestUtils.sha256Hex(
                        accessTokenResponse.getTokens().getBearerAccessToken().getValue()),
                capturedAccessTokenItem.getAccessToken());
        assertNotNull(capturedAccessTokenItem.getAccessTokenExpiryDateTime());
    }

    @Test
    void shouldGetSessionIdByAccessTokenWhenValidAccessTokenProvided() {
        String testResourceId = UUID.randomUUID().toString();
        AccessToken accessToken = new BearerAccessToken();
        String accessTokenValue = accessToken.toAuthorizationHeader();
        String testPassportSessionId = UUID.randomUUID().toString();

        AccessTokenItem accessTokenItem =
                new AccessTokenItem(
                        DigestUtils.sha256Hex(accessTokenValue),
                        testResourceId,
                        Instant.now().toString(),
                        testPassportSessionId);
        accessTokenItem.setResourceId(testResourceId);
        when(mockDataStore.getItem(DigestUtils.sha256Hex(accessTokenValue)))
                .thenReturn(accessTokenItem);

        AccessTokenItem result = accessTokenService.getAccessTokenItem(accessTokenValue);

        verify(mockDataStore).getItem(DigestUtils.sha256Hex(accessTokenValue));

        assertNotNull(result.getResourceId());
        assertEquals(testResourceId, result.getResourceId());
    }

    @Test
    void shouldReturnNullWhenInvalidAccessTokenProvided() {
        String accessToken = new BearerAccessToken().toAuthorizationHeader();

        when(mockDataStore.getItem(DigestUtils.sha256Hex(accessToken))).thenReturn(null);

        AccessTokenItem accessTokenItem = accessTokenService.getAccessTokenItem(accessToken);

        verify(mockDataStore).getItem(DigestUtils.sha256Hex(accessToken));
        assertNull(accessTokenItem);
    }

    @Test
    void shouldRevokeAccessToken() {
        String accessToken = "test-access-token";

        AccessTokenItem accessTokenItem =
                new AccessTokenItem(
                        accessToken,
                        UUID.randomUUID().toString(),
                        Instant.now().toString(),
                        UUID.randomUUID().toString());

        when(mockDataStore.getItem(accessToken)).thenReturn(accessTokenItem);

        accessTokenService.revokeAccessToken(accessToken);

        ArgumentCaptor<AccessTokenItem> accessTokenItemArgCaptor =
                ArgumentCaptor.forClass(AccessTokenItem.class);

        verify(mockDataStore).update(accessTokenItemArgCaptor.capture());
        assertNotNull(accessTokenItemArgCaptor.getValue().getRevokedAtDateTime());
    }

    @Test
    void shouldNotAttemptUpdateIfAccessTokenIsAlreadyRevoked() {
        String accessToken = "test-access-token";

        AccessTokenItem accessTokenItem =
                new AccessTokenItem(
                        accessToken,
                        UUID.randomUUID().toString(),
                        Instant.now().toString(),
                        UUID.randomUUID().toString());
        accessTokenItem.setRevokedAtDateTime(Instant.now().toString());

        when(mockDataStore.getItem(accessToken)).thenReturn(accessTokenItem);

        accessTokenService.revokeAccessToken(accessToken);

        verify(mockDataStore, Mockito.times(0)).update(any());
    }

    @Test
    void shouldThrowExceptionIfAccessTokenCanNotBeFoundWhenRevoking() {
        String accessToken = "test-access-token";

        when(mockDataStore.getItem(accessToken)).thenReturn(null);

        try {
            accessTokenService.revokeAccessToken(accessToken);
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertEquals(
                    "Failed to revoke access token - access token could not be found in DynamoDB",
                    e.getMessage());
        }
    }
}
