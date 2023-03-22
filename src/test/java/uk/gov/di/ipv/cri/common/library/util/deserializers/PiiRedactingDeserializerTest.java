package uk.gov.di.ipv.cri.common.library.util.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PiiRedactingDeserializerTest {
    private List<String> sensitiveFields = List.of("id", "name", "email");
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnNullWithNullRootNodeWhileAttemptingDeserialization() throws Exception {
        String json = "null";
        JsonDeserializer<? extends Person> piiRedactingDeserializer =
                setUpRedactionModule(
                        Person.class, () -> new PiiRedactingDeserializer<>(Person.class));

        Object result =
                piiRedactingDeserializer.deserialize(
                        jsonParser(json), getDefaultDeserializationContext());

        assertThat(result, nullValue());
    }

    @Test
    void shouldNotRedactValidJsonWhenSensitiveFieldsAreNotSpecified() throws IOException {
        String json =
                "{\"name\":\"John\",\"email\":\"john.doe@example.com\",\"phone\":\"123456789\",\"city\":\"New York\"}";
        JsonDeserializer<? extends Person> piiRedactingDeserializer =
                setUpRedactionModule(
                        Person.class, () -> new PiiRedactingDeserializer<>(Person.class));

        Person person =
                piiRedactingDeserializer.deserialize(
                        jsonParser(json), getDefaultDeserializationContext());

        assertThat(person.getName(), equalTo("John"));
        assertThat(person.getEmail(), equalTo("john.doe@example.com"));
        assertThat(person.getPhone(), equalTo("123456789"));
        assertThat(person.getCity(), equalTo("New York"));
    }

    @Test
    void shouldNotRedactValidJsonEvenWhenSensitiveFieldsAreSpecified() throws IOException {
        String json =
                "{\"name\":\"John\",\"email\":\"john.doe@example.com\",\"phone\":\"123456789\",\"city\":\"New York\"}";
        List<String> sensitiveFields = List.of("email", "phone");
        JsonDeserializer<? extends Person> piiRedactingDeserializer =
                setUpRedactionModule(
                        Person.class,
                        () -> new PiiRedactingDeserializer<>(sensitiveFields, Person.class));

        Person person =
                piiRedactingDeserializer.deserialize(
                        jsonParser(json), getDefaultDeserializationContext());

        assertThat(person.getName(), equalTo("John"));
        assertThat(person.getEmail(), equalTo("john.doe@example.com"));
        assertThat(person.getPhone(), equalTo("123456789"));
        assertThat(person.getCity(), equalTo("New York"));
    }

    @Test
    void shouldRedactAllFieldsWhenExceptionOccursDuringDeserializationDue() {
        String inValidJson =
                "{\"name\":\"John\",\"email\":\"john.doe@example.com\",\"age\":40,\"city\":\"New York\"}";
        JsonDeserializer<? extends Person> piiRedactingDeserializer =
                setUpRedactionModule(
                        Person.class, () -> new PiiRedactingDeserializer<>(Person.class));

        JsonMappingException exception =
                assertThrows(
                        JsonMappingException.class,
                        () ->
                                piiRedactingDeserializer.deserialize(
                                        jsonParser(inValidJson),
                                        getDefaultDeserializationContext()));

        assertThat(
                exception.getMessage(),
                containsString(
                        String.format(
                                "Error while deserializing object. Some PII fields were redacted. {\"name\":\"%s\",\"email\":\"%s\",\"age\":\"%s\",\"city\":\"%s\"}",
                                "*".repeat("John".length()),
                                "*".repeat("john.doe@example.com".length()),
                                "*".repeat("40".length()),
                                "*".repeat("New York".length()))));
    }

    @Test
    void shouldRedactSpecifiedFieldsWhenExceptionOccursDuringDeserializationDue() {
        String inValidJson =
                "{\"name\":\"John\",\"email\":\"john.doe@example.com\",\"age\":40,\"phone\":\"123456789\",\"city\":\"New York\"}";
        JsonDeserializer<? extends Person> piiRedactingDeserializer =
                setUpRedactionModule(
                        Person.class,
                        () -> new PiiRedactingDeserializer<>(sensitiveFields, Person.class));

        JsonMappingException exception =
                assertThrows(
                        JsonMappingException.class,
                        () ->
                                piiRedactingDeserializer.deserialize(
                                        jsonParser(inValidJson),
                                        getDefaultDeserializationContext()));

        assertThat(
                exception.getMessage(),
                containsString(
                        String.format(
                                "Error while deserializing object. Some PII fields were redacted. {\"name\":\"%s\",\"email\":\"%s\",\"age\":%d,\"phone\":\"%s\",\"city\":\"%s\"}",
                                "*".repeat("John".length()),
                                "*".repeat("john.doe@example.com".length()),
                                40,
                                "123456789",
                                "New York")));
    }

    @Test
    void shouldRedactFieldsInNestedObjects() {
        List<String> sensitiveFields = List.of("firstName", "lastName", "date");
        String InvalidNestedJson =
                "{\"name\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}],\"birthDate\":[{\"date\":\"1980-00-00\",\"place\":\"London\"}],\"address\":[{\"addressLine1\":\"123 Main St\",\"addressLine2\":\"\",\"city\":\"London\",\"postcode\":\"SW1A 1AA\",\"countryCode\":\"GB\"}],\"drivingPermit\":[{\"number\":\"AB12345\",\"type\":\"CAR\",\"expiryDate\":\"2023-00-00\"}]}";
        JsonDeserializer<? extends PersonIdentityDetailed> piiRedactingDeserializer =
                setUpRedactionModule(
                        PersonIdentityDetailed.class,
                        () ->
                                new PiiRedactingDeserializer<>(
                                        sensitiveFields, PersonIdentityDetailed.class));

        JsonMappingException exception =
                assertThrows(
                        JsonMappingException.class,
                        () ->
                                piiRedactingDeserializer.deserialize(
                                        jsonParser(InvalidNestedJson),
                                        getDefaultDeserializationContext()));

        assertThat(
                exception.getMessage(),
                containsString(
                        String.format(
                                "Error while deserializing object. Some PII fields were redacted. {\"name\":[{\"firstName\":\"%s\",\"lastName\":\"%s\"}],\"birthDate\":[{\"date\":\"%s\",\"place\":\"London\"}],\"address\":[{\"addressLine1\":\"123 Main St\",\"addressLine2\":\"\",\"city\":\"London\",\"postcode\":\"SW1A 1AA\",\"countryCode\":\"GB\"}],\"drivingPermit\":[{\"number\":\"AB12345\",\"type\":\"CAR\",\"expiryDate\":\"2023-00-00\"}]}",
                                "*".repeat("John".length()),
                                "*".repeat("Doe".length()),
                                "*".repeat("1980-00-00".length()))));
    }

    @Test
    void shouldRedactArrayFieldsWithEmptyString() {
        List<String> sensitiveFields = List.of("name", "address", "drivingPermit");
        String InvalidNestedJson =
                "{\"name\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}],\"birthDate\":[{\"date\":\"1980-00-00\",\"place\":\"London\"}],\"address\":[{\"addressLine1\":\"123 Main St\",\"addressLine2\":\"\",\"city\":\"London\",\"postcode\":\"SW1A 1AA\",\"countryCode\":\"GB\"}],\"drivingPermit\":[{\"number\":\"AB12345\",\"type\":\"CAR\",\"expiryDate\":\"2023-00-00\"}]}";
        JsonDeserializer<? extends PersonIdentityDetailed> piiRedactingDeserializer =
                setUpRedactionModule(
                        PersonIdentityDetailed.class,
                        () ->
                                new PiiRedactingDeserializer<>(
                                        sensitiveFields, PersonIdentityDetailed.class));

        JsonMappingException exception =
                assertThrows(
                        JsonMappingException.class,
                        () ->
                                piiRedactingDeserializer.deserialize(
                                        jsonParser(InvalidNestedJson),
                                        getDefaultDeserializationContext()));

        assertThat(
                exception.getMessage(),
                containsString(
                        String.format(
                                "Error while deserializing object. Some PII fields were redacted. {\"name\":\"%s\",\"birthDate\":[{\"date\":\"1980-00-00\",\"place\":\"London\"}],\"address\":\"%s\",\"drivingPermit\":\"%s\"}",
                                "*".repeat(6), "*".repeat(6), "*".repeat(6))));
    }

    private JsonParser jsonParser(String json) throws IOException {
        return objectMapper.getFactory().createParser(json);
    }

    private DefaultDeserializationContext getDefaultDeserializationContext() {
        JsonNode nullNode = objectMapper.getNodeFactory().nullNode();
        JsonParser nullParser = new TreeTraversingParser(nullNode);
        DeserializationConfig config = objectMapper.getDeserializationConfig();

        DefaultDeserializationContext context =
                new DefaultDeserializationContext.Impl(
                        objectMapper.getDeserializationContext().getFactory());
        context = context.createInstance(config, nullParser, objectMapper.getInjectableValues());
        return context;
    }

    private <T> JsonDeserializer<? extends T> setUpRedactionModule(
            Class<T> clazz, Supplier<JsonDeserializer<? extends T>> deserializerSupplier) {
        JsonDeserializer<? extends T> deserializer = deserializerSupplier.get();
        SimpleModule redactionModule = new SimpleModule();
        redactionModule.addDeserializer(clazz, deserializer);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()).registerModule(redactionModule);
        return deserializer;
    }

    static class Person {
        private String name;
        private String email;
        private String phone;
        private String city;

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        String getEmail() {
            return email;
        }

        void setEmail(String email) {
            this.email = email;
        }

        String getPhone() {
            return phone;
        }

        void setPhone(String phone) {
            this.phone = phone;
        }

        String getCity() {
            return city;
        }

        void setCity(String city) {
            this.city = city;
        }
    }
}
