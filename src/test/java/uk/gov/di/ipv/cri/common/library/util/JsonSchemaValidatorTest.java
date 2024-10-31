package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSchemaValidatorTest {
    private static final String VALID_JSON = "{ \"name\": \"John\", \"age\": 30 }";
    private static final String INVALID_JSON = "{ \"name\": \"John\", \"age\": \"thirty\" }";

    private static final String VALID_SCHEMA =
            "{\n"
                    + "  \"type\": \"object\",\n"
                    + "  \"properties\": {\n"
                    + "    \"name\": { \"type\": \"string\" },\n"
                    + "    \"age\": { \"type\": \"integer\" }\n"
                    + "  },\n"
                    + "  \"required\": [\"name\", \"age\"]\n"
                    + "}";

    private static final String INVALID_SCHEMA =
            "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"unknownType\" } } }";

    @Test
    void validateAgainstSchemaWithValidJsonAndSchemaReturnTrue() throws JsonProcessingException {
        final boolean result =
                JsonSchemaValidator.validateJsonAgainstSchema(VALID_JSON, VALID_SCHEMA);

        assertTrue(result, "Expected valid JSON to pass validation against the schema");
    }

    @Test
    void validateAgainstSchemaWithInvalidJsonReturnFalse() throws JsonProcessingException {
        final boolean result =
                JsonSchemaValidator.validateJsonAgainstSchema(INVALID_JSON, VALID_SCHEMA);

        assertFalse(result, "Expected invalid JSON to fail validation against the schema");
    }

    @Test
    void validateAgainstSchemaMissingRequiredFieldReturnFalse() throws JsonProcessingException {
        final String jsonWithMissingField = "{ \"name\": \"John\" }"; // Missing "age" field

        final boolean result =
                JsonSchemaValidator.validateJsonAgainstSchema(jsonWithMissingField, VALID_SCHEMA);

        assertFalse(result, "Expected JSON missing required fields to fail validation");
    }

    @Test
    void validateAgainstSchemaWithIncorrectJsonThrowsJsonProcessingException() {
        final String incorrectJson = "{ \"name\": \"John\", \"age\": 30 ";

        assertThrows(
                JsonProcessingException.class,
                () -> {
                    JsonSchemaValidator.validateJsonAgainstSchema(incorrectJson, VALID_SCHEMA);
                },
                "Expected incorrect JSON to throw JsonProcessingException");
    }

    @Test
    void validateAgainstSchemaWithInvalidSchemaThrowsValidationException() {
        assertThrows(
                Exception.class,
                () -> {
                    JsonSchemaValidator.validateJsonAgainstSchema(VALID_JSON, INVALID_SCHEMA);
                },
                "Expected invalid schema to throw an exception during validation");
    }
}
