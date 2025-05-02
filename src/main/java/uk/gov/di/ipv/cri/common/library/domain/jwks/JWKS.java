package uk.gov.di.ipv.cri.common.library.domain.jwks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JWKS {
    @JsonProperty("keys")
    private List<Key> keys;

    private int maxAgeFromCacheControlHeader;

    public JWKS() {
        maxAgeFromCacheControlHeader = 0;
    }

    public List<Key> getKeys() {
        return keys;
    }

    public void setKeys(List<Key> keys) {
        this.keys = keys;
    }

    public int getMaxAgeFromCacheControlHeader() {
        return maxAgeFromCacheControlHeader;
    }

    public void setMaxAgeFromCacheControlHeader(int maxAgeFromCacheControlHeader) {
        this.maxAgeFromCacheControlHeader = maxAgeFromCacheControlHeader;
    }
}
