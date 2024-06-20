package uk.gov.di.ipv.cri.common.library.persistence.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@DynamoDbBean
public class EvidenceRequest {
    private String scoringPolicy;
    private int strengthScore;
    private int validityScore;
    private int verificationScore;
    private int activityHistoryScore;
    private int identityFraudScore;

    public EvidenceRequest() {
        // Empty constructor for Jackson
    }

    public String getScoringPolicy() {
        return scoringPolicy;
    }

    public void setScoringPolicy(String scoringPolicy) {
        this.scoringPolicy = scoringPolicy;
    }

    public int getStrengthScore() {
        return strengthScore;
    }

    public void setStrengthScore(int strengthScore) {
        this.strengthScore = strengthScore;
    }

    public int getValidityScore() {
        return validityScore;
    }

    public void setValidityScore(int validityScore) {
        this.validityScore = validityScore;
    }

    public int getVerificationScore() {
        return verificationScore;
    }

    public void setVerificationScore(int verificationScore) {
        this.verificationScore = verificationScore;
    }

    public int getActivityHistoryScore() {
        return activityHistoryScore;
    }

    public void setActivityHistoryScore(int activityHistoryScore) {
        this.activityHistoryScore = activityHistoryScore;
    }

    public int getIdentityFraudScore() {
        return identityFraudScore;
    }

    public void setIdentityFraudScore(int identityFraudScore) {
        this.identityFraudScore = identityFraudScore;
    }
}
