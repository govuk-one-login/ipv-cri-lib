package uk.gov.di.ipv.cri.common.library.stepdefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import software.amazon.awssdk.http.HttpExecuteResponse;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.client.CommonApiClient;
import uk.gov.di.ipv.cri.common.library.client.StubClient;
import uk.gov.di.ipv.cri.common.library.client.TestResourcesClient;
import uk.gov.di.ipv.cri.common.library.helpers.SSMHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CommonSteps {
    private final ObjectMapper objectMapper;
    private final CommonApiClient commonApiClient;
    private final TestResourcesClient testResourcesClient;

    private final StubClient stubClient;
    private final CriTestContext testContext;

    private String sessionRequestBody;
    private String authorizationCode;
    private final ClientConfigurationService clientConfigurationService;

    public CommonSteps(
            ClientConfigurationService clientConfigurationService,
            CriTestContext testContext,
            SSMHelper ssmHelper) {
        this.stubClient = new StubClient(ssmHelper, clientConfigurationService);
        this.commonApiClient = new CommonApiClient(clientConfigurationService, ssmHelper);
        this.testResourcesClient =
                new TestResourcesClient(clientConfigurationService.getTestResourcesStackName());
        this.objectMapper = new ObjectMapper();
        this.testContext = testContext;
        this.clientConfigurationService = clientConfigurationService;
    }

    @Given("user has a default signed JWT")
    public void userHasADefaultSignedJWT() throws IOException, InterruptedException {
        HttpResponse<String> response = this.testResourcesClient.sendStartRequest();
        assertEquals(200, response.statusCode());
        sessionRequestBody = response.body();
    }

    @Given("user has an overridden signed JWT using {string}")
    public void userHasAnOverriddenSignedJWT(String claimOverrides)
            throws IOException, InterruptedException {

        HttpResponse<String> response =
                this.testResourcesClient.sendOverwrittenStartRequest(claimOverrides);

        assertEquals(200, response.statusCode());
        sessionRequestBody = response.body();
    }

    @When("user sends a POST request to session end point")
    public void userSendsAPostRequestToSessionEndpoint() throws IOException, InterruptedException {

        this.testContext.setResponse(this.commonApiClient.sendSessionRequest(sessionRequestBody));
        Map<String, String> deserializedResponse =
                objectMapper.readValue(
                        this.testContext.getResponse().body(), new TypeReference<>() {});
        this.testContext.setSessionId(deserializedResponse.get("session_id"));
    }

    @When("user sends a POST request to session end point with txma header")
    public void userSendsAPostRequestToSessionEndpointWithHeader()
            throws IOException, InterruptedException {

        this.testContext.setResponse(
                this.commonApiClient.sendNewSessionRequest(sessionRequestBody));
        Map<String, String> deserializedResponse =
                objectMapper.readValue(
                        this.testContext.getResponse().body(), new TypeReference<>() {});
        this.testContext.setSessionId(deserializedResponse.get("session_id"));
    }

    @When("user sends a GET request to authorization end point")
    public void userSendsAGetRequestToAuthorizationEndpoint()
            throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.commonApiClient.sendAuthorizationRequest(this.testContext.getSessionId()));
    }

    @When("user sends a POST request to token end point")
    public void userSendsAPostRequestToTokenEndpoint()
            throws IOException,
                    InterruptedException,
                    JOSEException,
                    ParseException,
                    URISyntaxException {
        PrivateKeyJWT privateKeyJWT =
                this.stubClient.generateClientAssertion(
                        clientConfigurationService.getDefaultClientId());
        var code = authorizationCode.trim();
        this.testContext.setResponse(this.commonApiClient.sendTokenRequest(privateKeyJWT, code));
    }

    @When("user sends a GET request to events end point for {string}")
    public void userSendsAGetRequestToEventsEndpoint(String eventName)
            throws IOException, InterruptedException {
        int maxAttempts = 5;
        int delayMillis = 500;

        HttpExecuteResponse response = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            response = testResourcesClient.sendEventRequest(testContext.getSessionId(), eventName);
            String responseBody = (String.valueOf(response.responseBody()));

            if (responseBody != null && !responseBody.isBlank()) {
                testContext.setResponse(response);
                return;
            }
            wait(delayMillis);
        }
        throw new AssertionError("No audit event body found for session");
    }

    @Then("user gets a session-id")
    public void userGetsASessionId() {
        assertNotNull(this.testContext.getSessionId());
    }

    @Then("user gets status code {int}")
    public void userGetsStatusCode(int statusCode) {
        assertEquals(statusCode, this.testContext.getResponse().statusCode());
    }

    @And("a valid authorization code is returned in the response")
    public void aValidAuthorizationCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        authorizationCode = jsonNode.get("authorizationCode").get("value").textValue();
        assertNotNull(authorizationCode);
    }

    @And("a valid access token code is returned in the response")
    public void aValidAccessTokenCodeIsReturnedInTheResponse() throws IOException {
        JsonNode jsonNode = objectMapper.readTree(this.testContext.getResponse().body());
        this.testContext.setAccessToken(jsonNode.get("access_token").asText());
        var expiresIn = jsonNode.get("expires_in").asInt();
        var tokenType = jsonNode.get("token_type").asText();
        assertEquals(3600, expiresIn);
        assertEquals("Bearer", tokenType);
        assertFalse(this.testContext.getAccessToken().isEmpty());
    }
}
