package uk.gov.di.ipv.cri.common.library.persistence.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ContextClaimConverterTest {

    private final String INT_USER_CONTEXT = "international_user";
    private final String UNKNOWN_CONTEXT = "unknown_context";

    private final ContextClaimConverter contextClaimConverter = new ContextClaimConverter();

    @Test
    public void shouldTransformEnumToAttributeString() {
        AttributeValue attributeIntUser =
                contextClaimConverter.transformFrom(ContextClaim.INTERNATIONAL_USER);
        assertEquals(attributeIntUser.s(), INT_USER_CONTEXT);

        AttributeValue attributeUnknown =
                contextClaimConverter.transformFrom(ContextClaim.UNKNOWN_CONTEXT);
        assertEquals(attributeUnknown.s(), UNKNOWN_CONTEXT);
    }

    @Test
    public void shouldTransformToEnumFromValidAttributeString() {
        ContextClaim intUserContext =
                contextClaimConverter.transformTo(AttributeValue.fromS(INT_USER_CONTEXT));
        assertEquals(intUserContext, ContextClaim.INTERNATIONAL_USER);

        ContextClaim unknownContext =
                contextClaimConverter.transformTo(AttributeValue.fromS(UNKNOWN_CONTEXT));
        assertEquals(unknownContext, ContextClaim.UNKNOWN_CONTEXT);
    }

    @Test
    public void shouldTransformToUnknownFromUnknownAttribute() {
        ContextClaim intUserContext =
                contextClaimConverter.transformTo(AttributeValue.fromS("Malformed Context"));
        assertEquals(intUserContext, ContextClaim.UNKNOWN_CONTEXT);

        ContextClaim UserContext = contextClaimConverter.transformTo(AttributeValue.fromNul(true));
        assertEquals(UserContext, ContextClaim.UNKNOWN_CONTEXT);
    }
}
