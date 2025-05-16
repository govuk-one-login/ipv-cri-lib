package uk.gov.di.ipv.cri.common.library.stepdefinitions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import uk.gov.di.ipv.cri.common.library.client.HttpHeaders;
import uk.gov.di.ipv.cri.common.library.client.TestResourcesClient;
import uk.gov.di.ipv.cri.common.library.config.ApiGatewayConfig;
import uk.gov.di.ipv.cri.common.library.config.CriStubClientConfig;
import uk.gov.di.ipv.cri.common.library.config.CriStubClientEnum;
import uk.gov.di.ipv.cri.common.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.common.library.helpers.HttpClientHelper;
import uk.gov.di.ipv.cri.common.library.helpers.HttpResponseHelper;
import uk.gov.di.ipv.cri.common.library.helpers.SSMHelper;
import uk.gov.di.ipv.cri.common.library.service.JWTDecrypter;
import uk.gov.di.ipv.cri.common.library.service.JWTVerifier;
import uk.gov.di.ipv.cri.common.library.service.KMSRSADecrypter;
import uk.gov.di.ipv.cri.common.library.util.EventProbe;
import uk.gov.di.ipv.cri.common.library.util.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.di.ipv.cri.common.library.config.EnvironConfig.getEnvOrDefault;

public class WellKnownJwksSteps {
    private static final String JSON_MIME_MEDIA_TYPE = "application/json";
    private final JWTDecrypter jwtDecrypter;
    private final HttpResponseHelper httpResponse;
    private final HttpClientHelper httpClientHelper;
    private final ObjectMapper objectMapper;
    private final ApiGatewayConfig apiGatewayConfig;
    private final TestResourcesClient testResourcesClient;
    private final SSMHelper ssmHelper;
    private final CriStubClientConfig stubClientConfig;
    private String publicKeyJwksBasePath;
    private SignedJWT decryptedJwt;

    public WellKnownJwksSteps() {
        httpClientHelper = new HttpClientHelper();
        httpResponse = new HttpResponseHelper();
        apiGatewayConfig = new ApiGatewayConfig();
        objectMapper = new ObjectMapper();
        ssmHelper = new SSMHelper(createSsmClient());
        stubClientConfig = new CriStubClientConfig(ssmHelper);

        testResourcesClient =
                new TestResourcesClient(
                        getEnvOrDefault("TEST_RESOURCES_STACK_NAME", "test-resources"));

        String authEncryptionKeyId =
                getParameter(
                        format(
                                "/%s/%s",
                                getEnvOrDefault("COMMON_STACK_NAME", "common-cri-api"),
                                "AuthRequestKmsEncryptionKeyId"));
        jwtDecrypter =
                new JWTDecrypter(
                        new KMSRSADecrypter(
                                authEncryptionKeyId, createKmsClient(), new EventProbe()));
    }

    @Given("that a public \\/.well-known\\/jwks.json endpoint exists for a CRI")
    public void thatAPublicWellKnownJwksJsonEndpointExistsForCRI() {
        publicKeyJwksBasePath = apiGatewayConfig.getPublicApiEndpoint();
    }

    @When("a request is made to fetch the public encryption keys")
    public void a_request_is_made_to_fetch_the_public_encryption_keys()
            throws IOException, InterruptedException {

        httpResponse.setResponse(
                httpClientHelper.sendHttpRequest(
                        HttpRequest.newBuilder()
                                .uri(getPublicEncryptionJwkUri())
                                .header(HttpHeaders.ACCEPT, JSON_MIME_MEDIA_TYPE)
                                .GET()
                                .build()));
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

    @And("at least one key is of use {string} and of type {string}")
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

    @When("request from the stub is decrypted using any valid key")
    public void usingAnyValidKey()
            throws IOException, InterruptedException, ParseException, JOSEException {

        HttpResponse<String> response = this.testResourcesClient.sendStartRequest();
        assertThat(200, is(response.statusCode()));

        JSONObject jwksResponse = new JSONObject(response.body());
        String jwe = jwksResponse.getString("request");

        decryptedJwt = jwtDecrypter.decrypt(jwe);
    }

    @SuppressWarnings("unchecked")
    @Then("that request is verified by the CRI")
    public void theRequestIsVerified() throws ParseException, ClientConfigurationException {
        verifyRequest(decryptedJwt);

        JWSHeader header = decryptedJwt.getHeader();

        assertThat(header.getAlgorithm().getName(), is("ES256"));
        assertThat(header.getType().getType(), is("JWT"));
        assertThat(header.getKeyID(), notNullValue());

        JWTClaimsSet claimsSet = decryptedJwt.getJWTClaimsSet();
        var claims = claimsSet.getClaims();

        var sharedClaims = (Map<String, Object>) claims.get("shared_claims");
        var names = (List<Map<String, Object>>) sharedClaims.get("name");
        var nameParts = (List<Map<String, Object>>) names.get(0).get("nameParts");
        var birthDates = (List<Map<String, Object>>) sharedClaims.get("birthDate");
        var addresses = (List<Map<String, Object>>) sharedClaims.get("address");

        assertThat(sharedClaims, allOf(hasKey("name"), hasKey("birthDate"), hasKey("address")));

        assertThat(names, everyItem(hasKey("nameParts")));
        assertThat(nameParts, everyItem(allOf(hasKey("type"), hasKey("value"))));
        assertThat(birthDates, everyItem(hasKey("value")));

        assertThat(
                addresses,
                everyItem(
                        allOf(
                                hasKey("addressLocality"),
                                hasKey("buildingNumber"),
                                hasKey("postalCode"),
                                hasKey("streetName"),
                                hasKey("validFrom"))));
    }

    private void verifyRequest(SignedJWT signedJWT) throws ClientConfigurationException {
        JWTVerifier jwtVerifier = new JWTVerifier();

        jwtVerifier.verifyAuthorizationJWT(getSSMClientConfig(), signedJWT);
    }

    private URI getPublicEncryptionJwkUri() {
        return new URIBuilder(publicKeyJwksBasePath)
                .setPath(this.httpClientHelper.createUriPath(".well-known/jwks.json"))
                .build();
    }

    private KmsClient createKmsClient() {
        return KmsClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    private SsmClient createSsmClient() {
        return SsmClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    private Map<String, String> getSSMClientConfig() {
        return Arrays.stream(CriStubClientEnum.values())
                .collect(toMap(CriStubClientEnum::getConfigName, stubClientConfig::getValue));
    }

    private String getParameter(String path) {
        return ssmHelper.getParameterValueByName(path);
    }
}
