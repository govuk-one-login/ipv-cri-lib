package uk.gov.di.ipv.cri.common.library.persistence.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ContextClaimTest {

    private final String INT_USER_CONTEXT = "international_user";
    private final String UNKNOWN_CONTEXT = "unknown_context";

    @Test
    void shouldCreateInternationalUserContextFromValidString() {
        assertEquals(ContextClaim.INTERNATIONAL_USER, ContextClaim.fromString(INT_USER_CONTEXT));
    }

    @Test
    void shouldCreateUnknownContextFromUnknownString() {
        assertEquals(ContextClaim.UNKNOWN_CONTEXT, ContextClaim.fromString(UNKNOWN_CONTEXT));
    }

    @Test
    void shouldCreateUnknownContextFromNull() {
        assertEquals(ContextClaim.UNKNOWN_CONTEXT, ContextClaim.fromString(null));
    }

    @Test
    void shouldConvertIntUserContextEnumToInternationalUserString() {
        assertEquals(INT_USER_CONTEXT, ContextClaim.INTERNATIONAL_USER.toString());
    }

    @Test
    void shouldConvertUnknownContextEnumToInternationalUserString() {
        assertEquals(UNKNOWN_CONTEXT, ContextClaim.UNKNOWN_CONTEXT.toString());
    }
}
