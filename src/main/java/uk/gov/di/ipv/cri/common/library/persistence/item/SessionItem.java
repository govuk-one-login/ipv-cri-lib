package uk.gov.di.ipv.cri.common.library.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.net.URI;
import java.util.UUID;

@DynamoDbBean
public class SessionItem {
    public static final String AUTHORIZATION_CODE_INDEX = "authorizationCode-index";
    public static final String ACCESS_TOKEN_INDEX = "access-token-index";
    private UUID sessionId;
    private long expiryDate;
    private long createdDate;
    private String clientId;
    private String state;
    private URI redirectUri;
    private String authorizationCode;
    private long authorizationCodeExpiryDate;
    private String accessToken;
    private long accessTokenExpiryDate;
    private String subject;
    private String persistentSessionId;
    private String clientSessionId;
    private String clientIpAddress;
    private int attemptCount;

    public SessionItem() {
        sessionId = UUID.randomUUID();
    }

    @DynamoDbPartitionKey()
    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = AUTHORIZATION_CODE_INDEX)
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setRedirectUri(URI redirectUri) {
        this.redirectUri = redirectUri;
    }

    public URI getRedirectUri() {
        return redirectUri;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = ACCESS_TOKEN_INDEX)
    public String getAccessToken() {
        return accessToken;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public long getAuthorizationCodeExpiryDate() {
        return authorizationCodeExpiryDate;
    }

    public void setAuthorizationCodeExpiryDate(long authorizationCodeExpiryDate) {
        this.authorizationCodeExpiryDate = authorizationCodeExpiryDate;
    }

    public long getAccessTokenExpiryDate() {
        return accessTokenExpiryDate;
    }

    public void setAccessTokenExpiryDate(long accessTokenExpiryDate) {
        this.accessTokenExpiryDate = accessTokenExpiryDate;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public String getPersistentSessionId() {
        return persistentSessionId;
    }

    public void setPersistentSessionId(String persistentSessionId) {
        this.persistentSessionId = persistentSessionId;
    }

    public String getClientSessionId() {
        return clientSessionId;
    }

    public void setClientSessionId(String clientSessionId) {
        this.clientSessionId = clientSessionId;
    }

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public void setClientIpAddress(String clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    @Override
    public String toString() {
        return "SessionItem{"
                + "sessionId="
                + sessionId
                + ", createdDate="
                + createdDate
                + ", clientId='"
                + clientId
                + '\''
                + ", state='"
                + state
                + '\''
                + ", redirectUri="
                + redirectUri
                + '}';
    }
}
