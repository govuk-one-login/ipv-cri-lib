package uk.gov.di.ipv.cri.common.library.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.di.ipv.cri.common.library.util.ListUtil.mergeDistinct;
import static uk.gov.di.ipv.cri.common.library.util.ListUtil.split;

/**
 * A utility class to read and delete messages from SQS queues in a predictable manner. The class is
 * a wrapper around {@link SqsClient} and allows to get around the message retrieval limitations of
 * the SQS service.
 */
public final class SQSHelper {

    /** The default total timeout for queue operations */
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /** Helper class to capture received messages */
    private static class FilteredMessages {
        public FilteredMessages(List<Message> matchingMessages, List<Message> nonMatchingMessages) {
            this.matchingMessages = Objects.requireNonNull(matchingMessages);
            this.nonMatchingMessages = nonMatchingMessages;
        }

        public List<Message> allMessages() {
            return Stream.of(matchingMessages, nonMatchingMessages)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        public final List<Message> matchingMessages;
        public final List<Message> nonMatchingMessages;
    }

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    /**
     * The total timeout for queue operations. Defaults to {@link SQSHelper#DEFAULT_TIMEOUT_SECONDS}
     */
    private int timeoutSeconds;

    private int sqsWaitTimeSeconds;
    private int sqsMessageVisibilityTimeout;

    /**
     * Use the {@link SQSHelper#DEFAULT_TIMEOUT_SECONDS default timeout}
     *
     * @see SQSHelper#SQSHelper(int, SqsClient, ObjectMapper)
     */
    public SQSHelper() {
        this(DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Specify a total timeout for queue operations
     *
     * @see SQSHelper#SQSHelper(int, SqsClient, ObjectMapper)
     */
    public SQSHelper(int timeoutSeconds) {
        this(timeoutSeconds, null, null);
    }

    /**
     * Optionally provide a custom SQS client and object mapper. The values can be {@code null} to
     * use the default constructors. The default timeout value can be accessed through {@link
     * SQSHelper#DEFAULT_TIMEOUT_SECONDS}
     */
    public SQSHelper(int timeoutSeconds, SqsClient sqsClient, ObjectMapper objectMapper) {
        this.setTimeout(timeoutSeconds);
        this.sqsClient = sqsClient == null ? SqsClient.create() : sqsClient;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * Set the total timeout for queue operations. Secondary timeouts are calculated based on the
     * provided value. The default timeout can be accessed through {@link
     * SQSHelper#DEFAULT_TIMEOUT_SECONDS}
     *
     * @param timeoutSeconds The desired total timeout to receive messages from an SQS queue
     * @implNote The maximum allowed wait time for {@link
     *     SqsClient#receiveMessage(ReceiveMessageRequest) receiveMessage} is 20 seconds. {@code
     *     sqsWaitTimeSeconds} is set to the maximum allowed value unless the total timeout is less
     *     than that.
     *     <p>{@code sqsMessageVisibilityTimeout} is set to the sum of the other two timeouts to
     *     allow overhead and ensure messages are not received twice within a single invocation.
     */
    public void setTimeout(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.sqsWaitTimeSeconds = Math.min(20, timeoutSeconds);
        this.sqsMessageVisibilityTimeout = this.timeoutSeconds + this.sqsWaitTimeSeconds + 5;
    }

    /**
     * Receive at least {@code count} messages from the queue. Messages are not hidden from
     * subsequent invocations.
     *
     * @param queueUrl The URL of the SQS queue
     * @param count The desired number of messages to receive. Exactly {@code count} or more
     *     messages are returned
     * @return The list of at least {@code count} received messages, or an empty list if {@code
     *     count} is less than 1
     * @throws InterruptedException If at least {@code count} messages have not been found within
     *     the {@link SQSHelper#timeoutSeconds timeout}
     * @implNote The received messages have their visibility timeout reset at the end of the
     *     invocation, so they can be received by subsequent invocations or calls to {@link
     *     SqsClient#receiveMessage(ReceiveMessageRequest) receiveMessage}
     * @see SqsClient#changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest)
     */
    public List<Message> receiveMessages(String queueUrl, int count) throws InterruptedException {
        return receiveMessages(queueUrl, count, null, false);
    }

    /**
     * Select messages from the queue based on the provided JSON body {@code filters} and receive at
     * least {@code count} matching messages.
     *
     * @param count The desired number of messages matching the provided {@code filters}. Exactly
     *     {@code count} or more messages are returned
     * @param filters Key-value pairs to filter and only select messages whose JSON body contents
     *     match all the provided pairs. Messages without a valid JSON body are ignored.
     * @return The list of at least {@code count} received messages matching the {@code filters}
     * @throws InterruptedException If at least {@code count} messages matching the filters have not
     *     been found within the {@link SQSHelper#timeoutSeconds timeout}
     * @see SQSHelper#receiveMessages(String, int)
     */
    public List<Message> receiveMatchingMessages(
            String queueUrl, int count, Map<String, String> filters) throws InterruptedException {
        return receiveMatchingMessages(queueUrl, count, filters, false);
    }

    /**
     * Select messages from the queue and optionally delete any other encountered messages not
     * matching the specified criteria.
     *
     * @param deleteNonMatching Set to {@code true} to delete any messages that were received from
     *     the queue but did not match the provided {@link SQSHelper#receiveMatchingMessages(String,
     *     int, Map) filters}. This does not clear the queue but deletes any additional messages
     *     received from SQS.
     * @see SQSHelper#receiveMatchingMessages(String, int, Map)
     */
    public List<Message> receiveMatchingMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching)
            throws InterruptedException {
        return receiveMessages(queueUrl, count, Objects.requireNonNull(filters), deleteNonMatching);
    }

    /**
     * Delete all messages from the provided list
     *
     * @see SqsClient#deleteMessageBatch(DeleteMessageBatchRequest)
     */
    public void deleteMessages(String queueUrl, List<Message> messages) {
        deleteMessagesFromList(queueUrl, messages);
    }

    /**
     * Delete a number of messages from the queue within the {@link SQSHelper#timeoutSeconds
     * timeout}. Messages are deleted in the order they're received from SQS. Exactly {@code count}
     * or more messages are deleted.
     *
     * @throws InterruptedException If at least {@code count} messages have not been found within
     *     the {@link SQSHelper#timeoutSeconds timeout}
     * @see SQSHelper#receiveMessages(String, int)
     */
    public void deleteMessages(String queueUrl, int count) throws InterruptedException {
        deleteMessages(queueUrl, count, null, false);
    }

    /**
     * Delete at least {@code count} messages from the queue based on the provided JSON body {@code
     * filters}.
     *
     * @throws InterruptedException If at least {@code count} messages matching the filters have not
     *     been found within the {@link SQSHelper#timeoutSeconds timeout}
     * @see SQSHelper#receiveMatchingMessages(String, int, Map)
     */
    public void deleteMatchingMessages(String queueUrl, int count, Map<String, String> filters)
            throws InterruptedException {
        deleteMatchingMessages(queueUrl, count, filters, false);
    }

    /**
     * Delete at least {@code count} messages from the queue and optionally delete any other
     * encountered messages not matching the specified criteria.
     *
     * @see SQSHelper#deleteMatchingMessages(String, int, Map)
     * @see SQSHelper#receiveMatchingMessages(String, int, Map, boolean)
     */
    public void deleteMatchingMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching)
            throws InterruptedException {
        deleteMessages(queueUrl, count, Objects.requireNonNull(filters), deleteNonMatching);
    }

    private List<Message> receiveMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching)
            throws InterruptedException {
        final FilteredMessages messages = getMessages(queueUrl, count, filters);

        if (deleteNonMatching) {
            deleteMessagesFromList(queueUrl, messages.nonMatchingMessages);
            resetMessageVisibility(queueUrl, messages.matchingMessages);
        } else {
            resetMessageVisibility(queueUrl, messages.allMessages());
        }

        return messages.matchingMessages;
    }

    private void deleteMessages(
            String queueUrl, int count, Map<String, String> filters, boolean deleteNonMatching)
            throws InterruptedException {
        final FilteredMessages messages = getMessages(queueUrl, count, filters);

        if (deleteNonMatching) {
            deleteMessagesFromList(queueUrl, messages.allMessages());
            return;
        }

        deleteMessagesFromList(queueUrl, messages.matchingMessages);

        if (filters != null) {
            resetMessageVisibility(queueUrl, messages.nonMatchingMessages);
        }
    }

    private FilteredMessages getMessages(String queueUrl, int count, Map<String, String> filters)
            throws InterruptedException {
        final boolean filterMessages = filters != null;
        final List<Message> targetMessages = new ArrayList<>();
        final List<Message> nonMatchingMessages = filterMessages ? new ArrayList<>() : null;

        final ReceiveMessageRequest receiveMessageRequest =
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(this.sqsWaitTimeSeconds)
                        .visibilityTimeout(this.sqsMessageVisibilityTimeout)
                        .build();

        final long startTime = System.currentTimeMillis();

        while (targetMessages.size() < count) {
            if (System.currentTimeMillis() - startTime >= timeoutSeconds * 1000L) {
                throw new InterruptedException(
                        String.format(
                                "Received %d/%d messages after %d seconds",
                                targetMessages.size(), count, timeoutSeconds));
            }

            final List<Message> newMessages =
                    this.sqsClient.receiveMessage(receiveMessageRequest).messages();

            if (filterMessages) {
                final Map<Boolean, List<Message>> messages =
                        newMessages.stream()
                                .collect(
                                        Collectors.partitioningBy(
                                                message -> messageMatches(message, filters)));

                mergeDistinct(targetMessages, messages.get(true), Message::messageId);
                mergeDistinct(nonMatchingMessages, messages.get(false), Message::messageId);
            } else {
                mergeDistinct(targetMessages, newMessages, Message::messageId);
            }
        }

        return new FilteredMessages(targetMessages, nonMatchingMessages);
    }

    private boolean messageMatches(Message message, Map<String, String> properties) {
        final JsonNode body = parseJson(message.body());
        return body != null
                && properties.keySet().stream()
                        .allMatch(key -> body.at(key).asText().equals(properties.get(key)));
    }

    private void deleteMessagesFromList(String queueUrl, List<Message> messages) {
        batchOperation(
                messages,
                batch ->
                        this.sqsClient.deleteMessageBatch(
                                DeleteMessageBatchRequest.builder()
                                        .queueUrl(queueUrl)
                                        .entries(deleteMessageBatchRequest(batch))
                                        .build()));
    }

    private void resetMessageVisibility(String queueUrl, List<Message> messages) {
        batchOperation(
                messages,
                batch ->
                        this.sqsClient.changeMessageVisibilityBatch(
                                ChangeMessageVisibilityBatchRequest.builder()
                                        .queueUrl(queueUrl)
                                        .entries(resetMessageVisibilityBatchRequest(batch))
                                        .build()));
    }

    private void batchOperation(List<Message> messages, Consumer<List<Message>> operation) {
        if (Objects.requireNonNull(messages).isEmpty()) {
            return;
        }

        split(messages, 10).forEach(operation);
    }

    private JsonNode parseJson(String json) {
        try {
            return this.objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static List<DeleteMessageBatchRequestEntry> deleteMessageBatchRequest(
            List<Message> messages) {
        return messages.stream()
                .map(
                        message ->
                                DeleteMessageBatchRequestEntry.builder()
                                        .id(message.messageId())
                                        .receiptHandle(message.receiptHandle())
                                        .build())
                .collect(Collectors.toList());
    }

    private static List<ChangeMessageVisibilityBatchRequestEntry>
            resetMessageVisibilityBatchRequest(List<Message> messages) {
        return messages.stream()
                .map(
                        message ->
                                ChangeMessageVisibilityBatchRequestEntry.builder()
                                        .id(message.messageId())
                                        .receiptHandle(message.receiptHandle())
                                        .visibilityTimeout(0)
                                        .build())
                .collect(Collectors.toList());
    }
}
