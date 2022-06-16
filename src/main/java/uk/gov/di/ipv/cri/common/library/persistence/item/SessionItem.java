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

    @Override
    public String toString() {
        return new StringBuilder()
                .append("AddressSessionItem{")
                .append("sessionId=")
                .append(sessionId)
                .append(", expiryDate=")
                .append(expiryDate)
                .append(", clientId='")
                .append(clientId)
                .append('\'')
                .append(", state='")
                .append(state)
                .append('\'')
                .append(", redirectUri=")
                .append(redirectUri)
                .append(", accessToken='")
                .append(accessToken)
                .append('\'')
                .append(", authorizationCode='")
                .append(authorizationCode)
                .append('\'')
                .append(", authorizationCodeExpiryDate='")
                .append(authorizationCodeExpiryDate)
                .append('\'')
                .append('}')
                .toString();
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }
}
