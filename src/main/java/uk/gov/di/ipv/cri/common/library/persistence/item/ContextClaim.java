package uk.gov.di.ipv.cri.common.library.persistence.item;

import java.util.Arrays;

public enum ContextClaim {
    INTERNATIONAL_USER("international_user"),
    UNKNOWN_CONTEXT("unknown_context");

    private final String text;

    ContextClaim(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public static ContextClaim fromString(String contextString) {
        return Arrays.stream(ContextClaim.values())
                .filter(c -> c.text.equalsIgnoreCase(contextString))
                .findFirst()
                .orElse(ContextClaim.UNKNOWN_CONTEXT);
    }
}
