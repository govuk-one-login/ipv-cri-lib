package uk.gov.di.ipv.cri.common.library.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

@DynamoDbBean
@ExcludeFromGeneratedCoverageReport
public class AuthorizationCodeItem {

    private String authCode;
    private String resourceId;
    private String redirectUrl;
    private String creationDateTime;
    private String issuedAccessToken;
    private String exchangeDateTime;
    private String passportSessionId;
    private long ttl;

    public AuthorizationCodeItem() {}

    // TODO: Clean up - remove this constructor after new auth code lambda is used
    public AuthorizationCodeItem(
            String authCode,
            String resourceId,
            String redirectUrl,
            String creationDateTime,
            String passportSessionId) {
        this.authCode = authCode;
        this.resourceId = resourceId;
        this.redirectUrl = redirectUrl;
        this.creationDateTime = creationDateTime;
        this.passportSessionId = passportSessionId;
    }

    public AuthorizationCodeItem(
            String authCode, String creationDateTime, String passportSessionId) {
        this.authCode = authCode;
        this.creationDateTime = creationDateTime;
        this.passportSessionId = passportSessionId;
    }

    @DynamoDbPartitionKey
    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(String creationDateTime) {
        this.creationDateTime = creationDateTime;
    }

    public String getIssuedAccessToken() {
        return issuedAccessToken;
    }

    public void setIssuedAccessToken(String issuedAccessToken) {
        this.issuedAccessToken = issuedAccessToken;
    }

    public String getExchangeDateTime() {
        return exchangeDateTime;
    }

    public void setExchangeDateTime(String exchangeDateTime) {
        this.exchangeDateTime = exchangeDateTime;
    }

    public String getPassportSessionId() {
        return passportSessionId;
    }

    public void setPassportSessionId(String passportSessionId) {
        this.passportSessionId = passportSessionId;
    }
}
