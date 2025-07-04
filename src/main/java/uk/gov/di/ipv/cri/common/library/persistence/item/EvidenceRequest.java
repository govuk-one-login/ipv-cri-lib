package uk.gov.di.ipv.cri.common.library.persistence.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@DynamoDbBean
public class EvidenceRequest {
    private String scoringPolicy;
    private Integer strengthScore;
    private Integer validityScore;
    private Integer verificationScore;
    private Integer activityHistoryScore;
    private Integer identityFraudScore;

    public EvidenceRequest() {
        // Empty constructor for Jackson
    }

    public String getScoringPolicy() {
        return scoringPolicy;
    }

    public void setScoringPolicy(String scoringPolicy) {
        this.scoringPolicy = scoringPolicy;
    }

    public Integer getStrengthScore() {
        return strengthScore;
    }

    public void setStrengthScore(Integer strengthScore) {
        this.strengthScore = strengthScore;
    }

    public Integer getValidityScore() {
        return validityScore;
    }

    public void setValidityScore(Integer validityScore) {
        this.validityScore = validityScore;
    }

    public Integer getVerificationScore() {
        return verificationScore;
    }

    public void setVerificationScore(Integer verificationScore) {
        this.verificationScore = verificationScore;
    }

    public Integer getActivityHistoryScore() {
        return activityHistoryScore;
    }

    public void setActivityHistoryScore(Integer activityHistoryScore) {
        this.activityHistoryScore = activityHistoryScore;
    }

    public Integer getIdentityFraudScore() {
        return identityFraudScore;
    }

    public void setIdentityFraudScore(Integer identityFraudScore) {
        this.identityFraudScore = identityFraudScore;
    }
}
