package uk.gov.di.ipv.cri.common.library.persistence;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import uk.gov.di.ipv.cri.common.library.util.ClientProviderFactory;

import java.util.List;
import java.util.stream.Collectors;

public class DataStore<T> {

    private final DynamoDbTable<T> table;
    private final Class<T> typeParameterClass;
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    public DataStore(
            String tableName,
            Class<T> typeParameterClass,
            DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.table =
                dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(typeParameterClass));
        this.typeParameterClass = typeParameterClass;
        this.dynamoDbEnhancedClient = dynamoDbEnhancedClient;
    }

    /**
     * To be removed as, calling this will not allow sharing a single DynamoDbEnhancedClient.
     *
     * @deprecated
     * @return DynamoDbEnhancedClient
     */
    @SuppressWarnings("java:S1133")
    @Deprecated(forRemoval = true)
    public static DynamoDbEnhancedClient getClient() {
        ClientProviderFactory clientProviderFactory = new ClientProviderFactory();

        return clientProviderFactory.getDynamoDbEnhancedClient();
    }

    public void create(T item) {
        this.table.putItem(item);
    }

    public void createItems(List<T> items) {
        WriteBatch putItemsBatch = createPutItemsWriteBatch(items);
        BatchWriteResult batchWriteResult = persistBatch(putItemsBatch);
        List<T> unprocessedItems = batchWriteResult.unprocessedPutItemsForTable(this.table);
        do {
            if (!unprocessedItems.isEmpty()) {
                batchWriteResult = persistBatch(createPutItemsWriteBatch(unprocessedItems));
                unprocessedItems = batchWriteResult.unprocessedPutItemsForTable(this.table);
            }
        } while (!unprocessedItems.isEmpty());
    }

    public T getItem(String partitionValue, String sortValue) {
        return getItemByKey(
                Key.builder().partitionValue(partitionValue).sortValue(sortValue).build());
    }

    public T getItem(String partitionValue) {
        return getItemByKey(Key.builder().partitionValue(partitionValue).build());
    }

    public List<T> getItems(String partitionValue) {
        return this.table
                .query(
                        QueryConditional.keyEqualTo(
                                Key.builder().partitionValue(partitionValue).build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public List<T> getItemByIndex(String indexName, String value) throws DynamoDbException {
        DynamoDbIndex<T> index = this.table.index(indexName);
        var attVal = AttributeValue.builder().s(value).build();
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(attVal).build());
        var queryEnhancedRequest =
                QueryEnhancedRequest.builder().queryConditional(queryConditional).build();

        return index.query(queryEnhancedRequest).stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public List<T> getItemsByAttribute(String attributeName, String attributeValue) {
        AttributeValue expressionValue = AttributeValue.builder().s(attributeValue).build();
        Expression attributeFilterExpression =
                Expression.builder()
                        .expression("#a = :b")
                        .putExpressionName("#a", attributeName)
                        .putExpressionValue(":b", expressionValue)
                        .build();

        return this.table.scan(r -> r.filterExpression(attributeFilterExpression)).stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    public T update(T item) {
        return this.table.updateItem(item);
    }

    public T delete(String partitionValue, String sortValue) {
        return delete(Key.builder().partitionValue(partitionValue).sortValue(sortValue).build());
    }

    public T delete(String partitionValue) {
        return delete(Key.builder().partitionValue(partitionValue).build());
    }

    private T getItemByKey(Key key) {
        return this.table.getItem(key);
    }

    private T delete(Key key) {
        return this.table.deleteItem(key);
    }

    private WriteBatch createPutItemsWriteBatch(List<T> items) {
        WriteBatch.Builder<T> builder =
                WriteBatch.builder(this.typeParameterClass).mappedTableResource(this.table);
        for (T item : items) {
            builder.addPutItem(r -> r.item(item).build());
        }
        return builder.build();
    }

    private BatchWriteResult persistBatch(WriteBatch writeBatch) {
        return this.dynamoDbEnhancedClient.batchWriteItem(
                BatchWriteItemEnhancedRequest.builder().writeBatches(writeBatch).build());
    }
}
