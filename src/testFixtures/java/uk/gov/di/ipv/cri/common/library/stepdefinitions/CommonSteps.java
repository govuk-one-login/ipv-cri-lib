package uk.gov.di.ipv.cri.common.library.stepdefinitions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import uk.gov.di.ipv.cri.common.library.client.ClientConfigurationService;
import uk.gov.di.ipv.cri.common.library.client.CommonApiClient;
import uk.gov.di.ipv.cri.common.library.client.IpvCoreStubClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CommonSteps {
    private final ObjectMapper objectMapper;
    private final CommonApiClient commonApiClient;

    private final IpvCoreStubClient ipvCoreStubClient;
    private final CriTestContext testContext;
    private final ClientConfigurationService clientConfigurationService;

    private String sessionRequestBody;
    private String authorizationCode;

    public CommonSteps(
            ClientConfigurationService clientConfigurationService, CriTestContext testContext) {
        this.clientConfigurationService = clientConfigurationService;
        this.commonApiClient = new CommonApiClient(clientConfigurationService);
        this.ipvCoreStubClient = new IpvCoreStubClient(clientConfigurationService);
        this.objectMapper = new ObjectMapper();
        this.testContext = testContext;
    }

    @Given("user has the test-identity {int} in the form of a signed JWT string")
    public void userHasTheTestIdentityInTheFormOfASignedJWTString(int testUserDataSheetRowNumber)
            throws IOException, InterruptedException {
        this.testContext.setSerialisedUserIdentity(
                this.ipvCoreStubClient.getClaimsForUser(
                        Map.of(
                                "cri", clientConfigurationService.getIpvCoreStubCriId(),
                                "rowNumber", String.valueOf(testUserDataSheetRowNumber))));
        sessionRequestBody =
                this.ipvCoreStubClient.createSessionRequest(
                        this.testContext.getSerialisedUserIdentity());
    }

    @Given(
            "user has the test-identity {int} and context of {string} in the form of a signed JWT string")
    public void userHasTheTestIdentityAndContextOfInTheFormOfASignedJWTString(
            int testUserDataSheetRowNumber, String userContext)
            throws IOException, InterruptedException {
        this.testContext.setSerialisedUserIdentity(
                this.ipvCoreStubClient.getClaimsForUser(
                        Map.of(
                                "cri", clientConfigurationService.getIpvCoreStubCriId(),
                                "rowNumber", String.valueOf(testUserDataSheetRowNumber),
                                "context", userContext)));
        sessionRequestBody =
                this.ipvCoreStubClient.createSessionRequest(
                        this.testContext.getSerialisedUserIdentity());
    }

    @Given(
            "user has the test-identity {int} and verificationScore of {int} in the form of a signed JWT string")
    public void userHasTheTestIdentityAndVerificationScoreInTheFormOfASignedJWTString(
            int testUserDataSheetRowNumber, int verificationScore)
            throws IOException, InterruptedException {

        this.testContext.setSerialisedUserIdentity(
                this.ipvCoreStubClient.getClaimsForUser(
                        Map.of(
                                "cri", clientConfigurationService.getIpvCoreStubCriId(),
                                "rowNumber", String.valueOf(testUserDataSheetRowNumber),
                                "scoringPolicy", "gpg45",
                                "verificationScore", String.valueOf(verificationScore))));

        sessionRequestBody =
                this.ipvCoreStubClient.createSessionRequest(
                        this.testContext.getSerialisedUserIdentity());
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
    public void userSendsAPostRequestToTokenEndpoint() throws IOException, InterruptedException {
        String privateKeyJWT =
                this.ipvCoreStubClient.getPrivateKeyJWTFormParamsForAuthCode(
                        authorizationCode.trim());
        this.testContext.setResponse(this.commonApiClient.sendTokenRequest(privateKeyJWT));
    }

    @When("user sends a GET request to events end point")
    public void userSendsAGetRequestToEventsEndpoint() throws IOException, InterruptedException {
        this.testContext.setResponse(
                this.commonApiClient.sendEventRequest(this.testContext.getSessionId()));
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
