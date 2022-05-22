package uk.gov.di.ipv.cri.common.library.domain.sharedclaims;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class NamePartTest {

    @Test
    void testThatUnknownFieldsAreIgnored() throws IOException {
        String invalidJSON =
                "{\n"
                        + "  \"type\": \"a type\",\n"
                        + "  \"validUntil\": 12345\n"
                        + // validUntil is not a property
                        "}";
        NamePart namePart =
                new ObjectMapper()
                        .readValue(invalidJSON.getBytes(StandardCharsets.UTF_8), NamePart.class);
        assertThat(namePart.getType(), equalTo("a type"));
    }
}
