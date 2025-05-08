package uk.gov.di.ipv.cri.common.library.stepdefinitions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.gov.di.ipv.cri.common.library.config.ApiGatewayConfig;
import uk.gov.di.ipv.cri.common.library.helpers.HttpClientHelper;
import uk.gov.di.ipv.cri.common.library.helpers.HttpResponseHelper;
import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

public class WellKnownJwksSteps {
    private static final String JSON_MIME_MEDIA_TYPE = "application/json";
    private String publicKeyJwksBasePath;
    private final HttpResponseHelper httpResponse;
    private final HttpClientHelper httpClientHelper;
    private final ObjectMapper objectMapper;
    private final ApiGatewayConfig apiGatewayConfig;

    public WellKnownJwksSteps() {
        this.httpClientHelper = new HttpClientHelper();
        this.httpResponse = new HttpResponseHelper();
        this.apiGatewayConfig = new ApiGatewayConfig();
        this.objectMapper = new ObjectMapper();
    }

    @Given("that a public \\/.well-known\\/jwks.json endpoint exists for CRI")
    public void thatAPublicWellKnownJwksJsonEndpointExistsForCRI() {
        publicKeyJwksBasePath = this.apiGatewayConfig.getPublicApiEndpoint();
    }

    @When("a request is made to fetch the public encryption keys")
    public void a_request_is_made_to_fetch_the_public_encryption_keys()
            throws IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                new URIBuilder(publicKeyJwksBasePath)
                                        .setPath(
                                                this.httpClientHelper.createUriPath(
                                                        ".well-known/jwks.json"))
                                        .build())
                        .header("Accept", JSON_MIME_MEDIA_TYPE)
                        .GET()
                        .build();

        httpResponse.setResponse(this.httpClientHelper.sendHttpRequest(request));
    }

    @Then("the response from the endpoint includes the public JWK keyset")
    public void the_response_from_the_endpoint_includes_the_public_jwk_keyset()
            throws JsonProcessingException {
        JsonNode jwkResponse = objectMapper.readTree(httpResponse.getResponse().body());

        assertThat(200, is(httpResponse.getResponse().statusCode()));
        assertThat(jwkResponse.has("keys"), is(true));
        assertThat(jwkResponse.get("keys").isArray(), is(true));
        assertThat(jwkResponse.get("keys").size(), greaterThan(0));

        JsonNode firstKey = jwkResponse.get("keys").get(0);
        assertThat(firstKey.get("kty").asText(), is("RSA"));
        assertThat(firstKey.get("use").asText(), is("enc"));
        assertThat(firstKey.get("alg").asText(), is("RSA_OAEP_256"));
    }

    @And("each key has an associated kid")
    public void each_key_has_an_associated_kid() throws JsonProcessingException {
        JsonNode jwkResponse = objectMapper.readTree(httpResponse.getResponse().body());

        for (JsonNode key : jwkResponse.get("keys")) {

            assertThat(key.has("n"), is(true));
            assertThat(key.get("n").asText(), not(emptyOrNullString()));

            assertThat(key.has("e"), is(true));
            assertThat(key.get("e").asText(), not(emptyOrNullString()));

            assertThat(key.has("kid"), is(true));
            assertThat(key.get("kid").asText(), not(emptyOrNullString()));
        }
    }

    @And("at least one key use is {string} for {string}")
    public void at_least_one_key_use_is_for(String keyUse, String keyType)
            throws JsonProcessingException {
        JsonNode jwkResponse = objectMapper.readTree(httpResponse.getResponse().body());

        boolean hasAKeyUse =
                StreamSupport.stream(jwkResponse.get("keys").spliterator(), false)
                        .anyMatch(key -> keyUse.equals(key.get("use").asText()));
        assertThat(
                "At least one key should be for" + keyType + "(use=" + keyUse + ")",
                hasAKeyUse,
                is(true));
    }
}
