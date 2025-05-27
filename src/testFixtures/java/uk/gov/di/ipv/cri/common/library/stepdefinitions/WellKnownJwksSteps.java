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
import software.amazon.awssdk.services.kms.model.InvalidCiphertextException;
import uk.gov.di.ipv.cri.common.library.client.HttpHeaders;
import uk.gov.di.ipv.cri.common.library.client.TestResourcesClient;
import uk.gov.di.ipv.cri.common.library.config.ApiGateway;
import uk.gov.di.ipv.cri.common.library.config.CriStubClientEnum;
import uk.gov.di.ipv.cri.common.library.exception.ClientConfigurationException;
import uk.gov.di.ipv.cri.common.library.helpers.HttpClientHelper;
import uk.gov.di.ipv.cri.common.library.helpers.HttpResponseHelper;
import uk.gov.di.ipv.cri.common.library.helpers.SSMHelper;
import uk.gov.di.ipv.cri.common.library.service.JWTDecrypter;
import uk.gov.di.ipv.cri.common.library.service.JWTVerifier;
import uk.gov.di.ipv.cri.common.library.service.KMSRSADecrypter;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;
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
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.di.ipv.cri.common.library.client.HttpHeaders.JSON_MIME_MEDIA_TYPE;
import static uk.gov.di.ipv.cri.common.library.config.Environment.getEnvOrDefault;

public class WellKnownJwksSteps {
    private JWTDecrypter jwtDecrypter;
    private final HttpResponseHelper httpResponse;
    private final HttpClientHelper httpClientHelper;
    private final ObjectMapper objectMapper;
    private final TestResourcesClient testResourcesClient;
    private final SSMHelper ssmHelper;
    private final String authEncryptionKeyId;
    private String publicKeyJwksBasePath;
    private SignedJWT decryptedJwt;
    private KMSRSADecrypter kmsRsaDecrypter;

    public WellKnownJwksSteps() {
        httpClientHelper = new HttpClientHelper();
        httpResponse = new HttpResponseHelper();
        objectMapper = new ObjectMapper();
        ssmHelper = new SSMHelper();

        testResourcesClient =
                new TestResourcesClient(
                        getEnvOrDefault("TEST_RESOURCES_STACK_NAME", "test-resources"));

        authEncryptionKeyId =
                ssmHelper.getParameterValue(
                        format(
                                "/%s/%s",
                                getEnvOrDefault("COMMON_STACK_NAME", "common-cri-api"),
                                "AuthRequestKmsEncryptionKeyId"));
    }

    @Given("that a public \\/.well-known\\/jwks.json endpoint exists for a CRI")
    public void thatAPublicWellKnownJwksJsonEndpointExistsForCRI() {
        publicKeyJwksBasePath = ApiGateway.getPublicApiEndpoint();
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

    @And("the feature flag is {string}")
    public void feature_flag(String value) {
        setKmsRsaDecrypter(
                new KMSRSADecrypter(
                        new ClientProviderFactory().getKMSClient(),
                        new EventProbe(),
                        authEncryptionKeyId,
                        value.equalsIgnoreCase("enabled") ? true : false));

        setJwtDecrypter(new JWTDecrypter(getKmsrsaDecrypter()));
    }

    private void setKmsRsaDecrypter(KMSRSADecrypter kmsRsaDecrypter) {
        this.kmsRsaDecrypter = kmsRsaDecrypter;
    }

    private KMSRSADecrypter getKmsrsaDecrypter() {
        return this.kmsRsaDecrypter;
    }

    @When("the core stub forms a request for the CRI's \\/session endpoint")
    public void usingAnyValidKey() throws IOException, InterruptedException {

        HttpResponse<String> response = this.testResourcesClient.sendStartRequest();
        assertThat(200, is(response.statusCode()));

        httpResponse.setResponse(response);
    }

    @When("the core stub does NOT make a call to the CRI's \\/well-known\\/jwks.json endpoint")
    public void the_core_stub_doesnt_call_the_well_known_endpoint() throws ParseException {
        try {
            JSONObject jwksResponse = new JSONObject(httpResponse.getResponse().body());
            String jwe = jwksResponse.getString("request");

            decryptedJwt = jwtDecrypter.decrypt(jwe);
        } catch (InvalidCiphertextException | JOSEException ice) {
            assertThat(kmsRsaDecrypter.isKeyRotationEnabled(), is(false));
        }
    }

    @When("the core stub makes a call to the CRI's \\/well-known\\/jwks.json endpoint")
    public void the_core_stub_makes_call_the_well_known_endpoint()
            throws ParseException, JOSEException {
        JSONObject jwksResponse = new JSONObject(httpResponse.getResponse().body());
        String jwe = jwksResponse.getString("request");

        decryptedJwt = jwtDecrypter.decrypt(jwe);

        assertThat(kmsRsaDecrypter.isKeyRotationEnabled(), is(true));
    }

    @Then("the request by the core stub is NOT verified by the CRI")
    public void theRequestIsNotVerified() {
        assertThat(decryptedJwt, is(nullValue()));
    }

    @SuppressWarnings("unchecked")
    @Then("the request by the core stub is verified by the CRI")
    public void theRequestByTheCoreStubIsVerifiedByTheCRI()
            throws ParseException, ClientConfigurationException {
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

    private void setJwtDecrypter(JWTDecrypter jwtDecrypter) {
        this.jwtDecrypter = jwtDecrypter;
    }

    private URI getPublicEncryptionJwkUri() {
        return new URIBuilder(publicKeyJwksBasePath)
                .setPath(this.httpClientHelper.createUriPath(".well-known/jwks.json"))
                .build();
    }

    private Map<String, String> getSSMClientConfig() {
        return Arrays.stream(CriStubClientEnum.values())
                .collect(toMap(CriStubClientEnum::getConfigName, ssmHelper::getParameterValue));
    }
}
