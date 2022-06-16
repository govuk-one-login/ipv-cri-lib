package uk.gov.di.ipv.cri.common.library.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditEventTest {

    @ParameterizedTest
    @MethodSource("valuesToAddToRestricted")
    void shouldAddRestrictedValue(String key, String value, boolean expected) {
        AuditEvent auditEvent = new AuditEvent(System.currentTimeMillis(), "_", "_");
        assertEquals(expected, auditEvent.addRestricted(key, value));
        assertEquals(expected, auditEvent.getRestricted().containsKey(key));
    }

    @Test
    void shouldNotMarshsallEmptyRestrictedMap() throws JsonProcessingException {
        String json =
                new ObjectMapper()
                        .writeValueAsString(new AuditEvent(System.currentTimeMillis(), "_", "_"));
        assertThat(json, Matchers.not(StringContains.containsString("restricted")));
    }

    @Test
    void shouldWriteRestrictedMap() throws JsonProcessingException {
        AuditEvent auditEvent = new AuditEvent(System.currentTimeMillis(), "_", "_");
        auditEvent.addRestricted("foo", "bar");
        String json = new ObjectMapper().writeValueAsString(auditEvent);
        assertThat(json, StringContains.containsString("restricted"));
    }

    private static Stream<Arguments> valuesToAddToRestricted() {
        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of("foo", " ", false),
                Arguments.of("foo", "bar", true));
    }
}
