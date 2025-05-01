package uk.gov.di.ipv.cri.common.library.domain.jwks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Key {
    @JsonProperty("kty")
    private String kty;

    @JsonProperty("e")
    private String e;

    @JsonProperty("use")
    private String use;

    @JsonProperty("crv")
    private String crv;

    @JsonProperty("alg")
    private String alg;

    @JsonProperty("n")
    private String n;

    @JsonProperty("x")
    private String x;

    @JsonProperty("y")
    private String y;

    @JsonProperty("kid")
    private String kid;

    public Key() {
        // Empty constructor for Jackson
    }

    public String getKty() {
        return kty;
    }

    public void setKty(String kty) {
        this.kty = kty;
    }

    public String getE() {
        return e;
    }

    public void setE(String e) {
        this.e = e;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getCrv() {
        return crv;
    }

    public void setCrv(String crv) {
        this.crv = crv;
    }
}
