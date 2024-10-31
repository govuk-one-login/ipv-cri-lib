package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

public class JsonSchemaValidator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonSchemaValidator() {}

    public static boolean validateJsonAgainstSchema(String json, String jsonSchema)
            throws JsonProcessingException {
        final JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
        final JsonNode jsonSchemaNode = OBJECT_MAPPER.readTree(jsonSchema);
        final JSONObject jsonSchemaObject = new JSONObject(jsonSchemaNode.toString());
        final Schema schema = SchemaLoader.load(jsonSchemaObject);

        try {
            schema.validate(new JSONObject(jsonNode.toString()));
        } catch (ValidationException e) {
            LOGGER.error("Failed to validate JSON against JSON schema", e);
            return false;
        }

        return true;
    }
}
