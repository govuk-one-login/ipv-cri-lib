package uk.gov.di.ipv.cri.common.library.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.persistence.item.CanonicalAddress;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
public class ObjectMapperTest {

    public final String TIME_MODULE_ID = "jackson-datatype-jsr310";
    public final String JAVA8_MODULE_ID = "com.fasterxml.jackson.datatype.jdk8.Jdk8Module";

    @Test
    void shouldCreateObjectMapperSuccessfully() {
        ObjectMapper objectMapper = new ObjectMapper();
        assertThat(objectMapper, notNullValue());
        assertThat(objectMapper.getRegisteredModuleIds(), hasItem(TIME_MODULE_ID));
        assertThat(objectMapper.getRegisteredModuleIds(), hasItem(JAVA8_MODULE_ID));
    }

    @Test
    void shouldCreateFromExistingMapperAndAddModules() {
        var existingMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        assertThat(existingMapper.getRegisteredModuleIds(), not(hasItem(TIME_MODULE_ID)));
        assertThat(existingMapper.getRegisteredModuleIds(), not(hasItem(JAVA8_MODULE_ID)));

        ObjectMapper objectMapper = new ObjectMapper(existingMapper);
        assertThat(objectMapper, notNullValue());
        assertThat(objectMapper.getRegisteredModuleIds(), hasItem(TIME_MODULE_ID));
        assertThat(objectMapper.getRegisteredModuleIds(), hasItem(JAVA8_MODULE_ID));
    }

    @Test
    void shouldNoLongerThrowErrors() {
        CanonicalAddress address = new CanonicalAddress();
        address.setStreetName("street");
        address.setPostalCode("postcode");
        address.setAddressLocality("town");
        address.setValidFrom(LocalDate.now());

        var existingMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        assertThrows(
                JsonProcessingException.class, () -> existingMapper.writeValueAsString(address));

        ObjectMapper objectMapper = new ObjectMapper(existingMapper);
        assertDoesNotThrow(() -> objectMapper.writeValueAsString(address));
    }

    @Test
    void shouldCreateFromExistingMapperAndLeaveExistingModules() {
        var existingMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        existingMapper.registerModule(new Jdk8Module());
        existingMapper.registerModule(new JavaTimeModule());

        assertThat(existingMapper.getRegisteredModuleIds(), hasItem(TIME_MODULE_ID));
        assertThat(existingMapper.getRegisteredModuleIds(), hasItem(JAVA8_MODULE_ID));

        ObjectMapper objectMapper = new ObjectMapper(existingMapper);
        assertThat(objectMapper, notNullValue());
        assertThat(objectMapper.getRegisteredModuleIds(), hasItem(TIME_MODULE_ID));
        assertThat(objectMapper.getRegisteredModuleIds(), hasItem(JAVA8_MODULE_ID));
    }

    @Test
    void shouldCorrectlySerializeOptional() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String optionalString = objectMapper.writeValueAsString(Optional.of(1));
        String emptyOptionalString = objectMapper.writeValueAsString(Optional.empty());
        assertThat(optionalString, is("1"));
        assertThat(emptyOptionalString, is("null"));
    }

    @Test
    void shouldConvertToMap() {
        CanonicalAddress address = new CanonicalAddress();
        address.setStreetName("street");
        address.setPostalCode("postcode");
        address.setAddressLocality("town");
        address.setValidFrom(LocalDate.now());

        ObjectMapper objectMapper = new ObjectMapper();
        var map = objectMapper.toMap(address);

        assertThat(map, notNullValue());
    }

    @Test
    void shouldConvertToMapWithoutStreetName() {
        CanonicalAddress address = new CanonicalAddress();
        address.setStreetName("street");
        address.setPostalCode("postcode");
        address.setAddressLocality("town");
        address.setValidFrom(LocalDate.now());

        ObjectMapper objectMapper = new ObjectMapper();
        var map = objectMapper.toMap(address, List.of("streetName"));

        assertThat(map, notNullValue());
        assertFalse(map.containsKey("streetName"));
    }

    @Test
    void shouldConvertToMapWithOnlyStreetName() {
        CanonicalAddress address = new CanonicalAddress();
        address.setStreetName("street");
        address.setPostalCode("postcode");
        address.setAddressLocality("town");
        address.setValidFrom(LocalDate.now());

        ObjectMapper objectMapper = new ObjectMapper();
        var map = objectMapper.toMap(address, List.of("streetName"), true);

        assertThat(map, notNullValue());
        assertFalse(map.containsKey("uprn"));
        assertFalse(map.containsKey("street"));
        assertFalse(map.containsKey("postcode"));
        assertFalse(map.containsKey("validFrom"));
        assertTrue(map.containsKey("streetName"));
    }

    @Test
    void shouldConvertToMapWithoutNulls() {
        CanonicalAddress address = new CanonicalAddress();
        address.setStreetName("street");
        address.setPostalCode(null);
        address.setAddressLocality("town");
        address.setValidFrom(LocalDate.now());

        ObjectMapper objectMapper = new ObjectMapper();
        var map = objectMapper.toMap(address);

        assertFalse(map.containsKey("uprn"));
        assertFalse(map.containsKey("postalCode"));
    }
}
