package uk.gov.di.ipv.cri.common.library.persistence.item;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ContextClaimConverter implements AttributeConverter<ContextClaim> {

    @Override
    public AttributeValue transformFrom(ContextClaim context) {
        return AttributeValue.fromS(context.toString());
    }

    @Override
    public ContextClaim transformTo(AttributeValue attributeValue) {
        return ContextClaim.fromString(attributeValue.s());
    }

    @Override
    public EnhancedType<ContextClaim> type() {
        return EnhancedType.of(ContextClaim.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
