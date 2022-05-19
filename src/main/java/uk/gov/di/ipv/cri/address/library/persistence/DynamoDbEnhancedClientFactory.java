package uk.gov.di.ipv.cri.address.library.persistence;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbEnhancedClientFactory {

    private final DynamoDbEnhancedClient client;

    public DynamoDbEnhancedClientFactory() {
        DynamoDbClient dynamoDbClient =
                DynamoDbClient.builder()
                        .httpClient(UrlConnectionHttpClient.create())
                        .region(Region.EU_WEST_2)
                        .build();
        this.client = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    public DynamoDbEnhancedClient getClient() {
        return this.client;
    }
}
