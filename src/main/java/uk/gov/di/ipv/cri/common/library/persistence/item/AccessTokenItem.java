package uk.gov.di.ipv.cri.common.library.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import uk.gov.di.ipv.cri.common.library.annotations.ExcludeFromGeneratedCoverageReport;

@DynamoDbBean
@ExcludeFromGeneratedCoverageReport
public class AccessTokenItem implements DynamodbItem {
    private String accessToken;
    private String accessTokenExpiryDateTime;
    private String resourceId;
    private String revokedAtDateTime;
    private String passportSessionId;
    private long ttl;

    // required for DynamoDb BeanTableSchema
    public AccessTokenItem() {}

    public AccessTokenItem(
            String accessToken,
            String resourceId,
            String accessTokenExpiryDateTime,
            String passportSessionId) {
        this.accessToken = accessToken;
        this.resourceId = resourceId;
        this.accessTokenExpiryDateTime = accessTokenExpiryDateTime;
        this.passportSessionId = passportSessionId;
    }

    @DynamoDbPartitionKey
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessTokenExpiryDateTime() {
        return accessTokenExpiryDateTime;
    }

    public void setAccessTokenExpiryDateTime(String accessTokenExpiryDateTime) {
        this.accessTokenExpiryDateTime = accessTokenExpiryDateTime;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getRevokedAtDateTime() {
        return revokedAtDateTime;
    }

    public void setRevokedAtDateTime(String revokedAtDateTime) {
        this.revokedAtDateTime = revokedAtDateTime;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getPassportSessionId() {
        return passportSessionId;
    }

    public void setPassportSessionId(String passportSessionId) {
        this.passportSessionId = passportSessionId;
    }
}
