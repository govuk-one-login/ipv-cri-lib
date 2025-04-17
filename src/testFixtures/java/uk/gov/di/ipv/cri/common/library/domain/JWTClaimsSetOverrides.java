package uk.gov.di.ipv.cri.common.library.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

//TODO - This may not actually be needed.
public class JWTClaimsSetOverrides {

    @JsonProperty("client_id")
    private String clientId;
    private String iss;
    private String sub;
    private String aud;
    private String iat;
    private String exp;
    private String nbf;
    @JsonProperty("response_type")
    private String responseType;
    @JsonProperty("redirect_uri")
    private String redirectUri;
    private String state;
    @JsonProperty("govuk_signin_journey_id")
    private String govukSigninJourneyId;
    @JsonProperty("shared_claims")
    private Object sharedClaims;
    @JsonProperty("evidence_requested")
    private Object evidenceRequested;
    private String context;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getAud() {
        return aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    public String getIat() {
        return iat;
    }

    public void setIat(String iat) {
        this.iat = iat;
    }

    public String getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = exp;
    }

    public String getNbf() {
        return nbf;
    }

    public void setNbf(String nbf) {
        this.nbf = nbf;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getGovukSigninJourneyId() {
        return govukSigninJourneyId;
    }

    public void setGovukSigninJourneyId(String govukSigninJourneyId) {
        this.govukSigninJourneyId = govukSigninJourneyId;
    }

    public Object getSharedClaims() {
        return sharedClaims;
    }

    public void setSharedClaims(Object sharedClaims) {
        this.sharedClaims = sharedClaims;
    }

    public Object getEvidenceRequested() {
        return evidenceRequested;
    }

    public void setEvidenceRequested(Object evidenceRequested) {
        this.evidenceRequested = evidenceRequested;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

}
