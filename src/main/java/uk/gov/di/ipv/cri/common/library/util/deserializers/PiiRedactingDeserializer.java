package uk.gov.di.ipv.cri.common.library.util.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class PiiRedactingDeserializer<T> extends JsonDeserializer<T> {
    private final List<String> sensitiveFields;
    private final Class<T> clazz;
    private final boolean allFieldsAreSensitiveOption;

    public PiiRedactingDeserializer(Class<T> clazz) {
        this(Collections.emptyList(), clazz);
    }

    public PiiRedactingDeserializer(List<String> sensitiveFields, Class<T> clazz) {
        this(sensitiveFields, clazz, true);
    }

    public PiiRedactingDeserializer(
            List<String> sensitiveFields, Class<T> clazz, boolean allFieldsAreSensitiveOption) {
        this.sensitiveFields = sensitiveFields;
        this.clazz = clazz;
        this.allFieldsAreSensitiveOption = allFieldsAreSensitiveOption;
    }

    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser parser, DeserializationContext context)
            throws IOException, NullPointerException {
        JsonNode rootNode = null;
        try {
            ObjectMapper objectMapper = (ObjectMapper) parser.getCodec();
            rootNode = objectMapper.readTree(parser);
            if (rootNode == null || rootNode.isNull()) {
                return null;
            }

            DeserializationConfig config = context.getConfig();
            JavaType type = TypeFactory.defaultInstance().constructType(clazz);
            JsonDeserializer<?> defaultDeserializer =
                    BeanDeserializerFactory.instance.buildBeanDeserializer(
                            context, type, config.introspect(type));

            if (defaultDeserializer instanceof ResolvableDeserializer) {
                ((ResolvableDeserializer) defaultDeserializer).resolve(context);
            }

            JsonParser treeParser = objectMapper.treeAsTokens(rootNode);
            config.initialize(treeParser);

            if (treeParser.getCurrentToken() == null) {
                treeParser.nextToken();
            }

            return (T) defaultDeserializer.deserialize(treeParser, context);
        } catch (IOException e) {
            throw JsonMappingException.from(
                    new TreeTraversingParser(null),
                    "Error while deserializing object. Some PII fields were redacted. "
                            + processNode(rootNode, applySensitivity()));
        }
    }

    private Predicate<String> applySensitivity() {
        return sensitiveFields.isEmpty()
                ? defaultAllFieldsAsSensitive -> allFieldsAreSensitiveOption
                : sensitiveFields::contains;
    }

    private JsonNode processNode(JsonNode node, Predicate<String> sensitivityTest) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            return processArrayNode(node, sensitivityTest);
        } else if (node.isObject()) {
            return processObjectNode((ObjectNode) node, sensitivityTest);
        } else {
            return node;
        }
    }

    private ArrayNode processArrayNode(JsonNode arrayNode, Predicate<String> sensitivityTest) {
        ArrayNode newArrayNode = new ArrayNode(JsonNodeFactory.instance);

        for (JsonNode node : arrayNode) {
            if (node.isValueNode()) {
                newArrayNode.add(node);
            } else {
                newArrayNode.add(processNode(node, sensitivityTest));
            }
        }

        return newArrayNode;
    }

    private ObjectNode processObjectNode(ObjectNode objectNode, Predicate<String> sensitivityTest) {
        ObjectNode processedNode = objectNode.deepCopy();

        Iterator<Map.Entry<String, JsonNode>> fieldIterator = processedNode.fields();
        while (fieldIterator.hasNext()) {
            Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
            String fieldName = fieldEntry.getKey();
            JsonNode fieldValue = fieldEntry.getValue();

            if (sensitivityTest.test(fieldName)) {
                redactField(processedNode, fieldName, fieldValue.asText());
            } else if (fieldValue.isObject()) {
                // recursively process object nodes
                processedNode.set(
                        fieldName, processObjectNode((ObjectNode) fieldValue, sensitivityTest));
            } else if (fieldValue.isArray()) {
                // recursively process array nodes
                processedNode.set(fieldName, processArrayNode(fieldValue, sensitivityTest));
            }
        }
        return processedNode;
    }

    private void redactField(ObjectNode objectNode, String field, String value) {
        objectNode.put(field, "*".repeat(value.length() == 0 ? 6 : value.length()));
    }
}
