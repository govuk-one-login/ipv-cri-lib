package uk.gov.di.ipv.cri.common.library.aws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES;
import static uk.gov.di.ipv.cri.common.library.util.ListUtil.split;

/**
 * A utility class to read and delete messages from SQS queues in a predictable manner. The class is
 * a wrapper around {@link SqsClient} and allows to get around the message retrieval limitations of
 * the SQS service.
 *
 * <p>The helper has been designed to allow multiple clients to read messages from the same queue
 * concurrently. While it doesn't implement a proper solution with locks and waiting, it attempts to
 * remediate most common issues and is suitable for use in test scenarios with these caveats:
 *
 * <ul>
 *   <li>When retrieving specific messages from the queue using the filters, each client should use
 *       filters with different values to prevent blocking and timeouts.
 *   <li>Any client may receive and hide messages that another client is trying to get a hold of.
 *       The logic in the helper makes clients release unwanted messages when there are no more
 *       messages available in the queue to prevent timeouts. However, this means that the more
 *       concurrent clients there are, the longer it will take each of them to collect the messages
 *       they are targeting since they each need to process the queue multiple times.
 *   <li>The queue should have DLQ disabled since SQS will move any messages that have been received
 *       <a
 *       href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-sqs-queue.html#cfn-sqs-queue-redrivepolicy">maxReceiveCount</a>
 *       times to the DLQ and render it unavailable to the clients reading from the queue.
 * </ul>
 */
public final class SQSHelper {

    private static final int SQS_WAIT_TIME = 20;

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    public SQSHelper() {
        this(null, null);
    }

    /**
     * Optionally provide a custom SQS client and object mapper. The values can be {@code null} to
     * use the default constructors.
     */
    public SQSHelper(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient == null ? SqsClient.create() : sqsClient;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
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
     * @throws InterruptedException If at least {@code count} messages have not been found after
     *     processing the queue
     * @implNote The received messages have their visibility timeout reset at the end of the
     *     invocation, so they can be received by subsequent invocations or calls to {@link
     *     SqsClient#receiveMessage(ReceiveMessageRequest) receiveMessage}
     * @see SqsClient#changeMessageVisibilityBatch(ChangeMessageVisibilityBatchRequest)
     */
    public List<Message> receiveMessages(String queueUrl, int count) throws InterruptedException {
        return receiveMessages(queueUrl, count, null, true);
    }

    /**
     * Select messages from the queue based on the provided JSON body {@code filters} and receive at
     * least {@code count} matching messages.
     *
     * @param count The desired number of messages matching the provided {@code filters}. Exactly
     *     {@code count} or more messages are returned
     * @param filters Key-value pairs to filter and only select messages whose JSON body contents
     *     match all the provided pairs. Messages without a valid JSON body are ignored.
     *     <p>The keys are specified as expressions to compile into instances of {@link
     *     com.fasterxml.jackson.core.JsonPointer JsonPointer}. The expressions must start with a
     *     {@code /} and use the same token as the sub-key separator. For instance, {@code
     *     /key/sub-key}.
     * @return The list of at least {@code count} received messages matching the {@code filters}
     * @throws InterruptedException If at least {@code count} messages matching the filters have not
     *     been found after processing the queue
     * @see SQSHelper#receiveMessages(String, int)
     */
    public List<Message> receiveMatchingMessages(
            String queueUrl, int count, Map<String, String> filters) throws InterruptedException {
        return receiveMessages(queueUrl, count, Objects.requireNonNull(filters), true);
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
     * Delete a number of messages from the queue in the order they're received from SQS. Exactly
     * {@code count} or more messages are deleted.
     *
     * @throws InterruptedException If at least {@code count} messages have not been found after
     *     processing the queue
     * @see SQSHelper#receiveMessages(String, int)
     */
    public void deleteMessages(String queueUrl, int count) throws InterruptedException {
        deleteMessages(queueUrl, count, null, true);
    }

    /**
     * Attempt to delete at least {@code count} messages from the queue. Any received messages are
     * deleted until the required number is reached or the queue has been processed. If it has not
     * been possible to retrieve the specified number of messages, any encountered messages are
     * deleted and the method returns without an error.
     *
     * @return The number of deleted messages
     * @see SQSHelper#deleteMessages(String, int)
     * @see SQSHelper#receiveMessages(String, int)
     */
    public int tryDeleteMessages(String queueUrl, int count) {
        return tryDeleteMessages(queueUrl, count, null);
    }

    /**
     * Delete at least {@code count} messages from the queue based on the provided JSON body {@code
     * filters}.
     *
     * @throws InterruptedException If at least {@code count} messages matching the filters have not
     *     been found after processing the queue
     * @see SQSHelper#deleteMessages(String, int)
     * @see SQSHelper#receiveMatchingMessages(String, int, Map)
     */
    public void deleteMatchingMessages(String queueUrl, int count, Map<String, String> filters)
            throws InterruptedException {
        deleteMessages(queueUrl, count, Objects.requireNonNull(filters), true);
    }

    /**
     * Attempt to delete at least {@code count} messages matching the {@code filters} and optionally
     * throw if the required number of messages has not been found after processing the queue.
     *
     * @return The number of deleted messages
     * @see SQSHelper#tryDeleteMessages(String, int)
     * @see SQSHelper#receiveMatchingMessages(String, int, Map)
     */
    public int tryDeleteMatchingMessages(String queueUrl, int count, Map<String, String> filters) {
        return tryDeleteMessages(queueUrl, count, Objects.requireNonNull(filters));
    }

    private int deleteMessages(
            String queueUrl, int count, Map<String, String> filters, boolean throwOnTimeout)
            throws InterruptedException {
        final var messages = receiveMessages(queueUrl, count, filters, throwOnTimeout);
        deleteMessagesFromList(queueUrl, messages);
        return messages.size();
    }

    private int tryDeleteMessages(String queueUrl, int count, Map<String, String> filters) {
        int deletedMessageCount;

        try {
            deletedMessageCount = deleteMessages(queueUrl, count, filters, false);
        } catch (InterruptedException e) {
            return 0;
        }

        return deletedMessageCount;
    }

    private List<Message> receiveMessages(
            String queueUrl, int count, Map<String, String> filters, boolean throwOnTimeout)
            throws InterruptedException {
        final var messages = getMessages(queueUrl, count, filters);

        if (messages.size() < count && throwOnTimeout) {
            throw new InterruptedException(
                    String.format("Received %d/%d messages", messages.size(), count));
        }

        return messages;
    }

    private List<Message> getMessages(String queueUrl, int count, Map<String, String> filters) {
        final var targetMessages = new HashMap<String, Message>();
        final var seenMessageIds = new HashSet<String>();

        int queueSize;
        int queueProcessingTime;
        boolean newMessagesReceived;
        HashSet<String> receivedMessageIds;

        do {
            queueSize = getQueueSize(queueUrl);
            queueProcessingTime = getQueueProcessingTime(queueSize);

            final var start =
                    changeMessageVisibility(
                            queueUrl, getVisibilityTimeout(queueProcessingTime), targetMessages);

            receivedMessageIds =
                    processQueue(
                            queueUrl, start, queueProcessingTime, count, filters, targetMessages);

            newMessagesReceived = newMessagesReceived(receivedMessageIds, seenMessageIds);
            seenMessageIds.addAll(receivedMessageIds);
        } while (targetMessages.size() < count
                && (!queueProcessed(queueSize, receivedMessageIds.size()) || newMessagesReceived));

        resetMessageVisibility(queueUrl, targetMessages);
        return List.copyOf(targetMessages.values());
    }

    private HashSet<String> processQueue(
            String queueUrl,
            Instant start,
            int queueProcessingTime,
            int targetMessageCount,
            Map<String, String> filters,
            HashMap<String, Message> targetMessages) {
        final var requestBuilder =
                ReceiveMessageRequest.builder()
                        .queueUrl(Objects.requireNonNull(queueUrl))
                        .waitTimeSeconds(SQS_WAIT_TIME)
                        .maxNumberOfMessages(10);

        final boolean filterMessages = filters != null;
        final var unwantedMessages = filterMessages ? new HashMap<String, Message>() : null;
        final var receivedMessageIds = new HashSet<String>();
        List<Message> newMessages;

        do {
            newMessages = receiveNewMessages(requestBuilder, queueProcessingTime, start);
            receivedMessageIds.addAll(
                    processNewMessages(newMessages, filters, targetMessages, unwantedMessages));
        } while (!newMessages.isEmpty()
                && targetMessages.size() < targetMessageCount
                && elapsedTime(start) < queueProcessingTime);

        if (filterMessages) {
            resetMessageVisibility(queueUrl, unwantedMessages);
        }

        return receivedMessageIds;
    }

    private Set<String> processNewMessages(
            List<Message> newMessages,
            Map<String, String> filters,
            Map<String, Message> targetMessages,
            Map<String, Message> unwantedMessages) {
        if (filters != null) {
            final var matching = sortMessages(newMessages, filters);
            final var matchingMessageIds = mergeMessages(targetMessages, matching.get(true));
            final var unwantedMessageIds = mergeMessages(unwantedMessages, matching.get(false));

            return Stream.of(matchingMessageIds, unwantedMessageIds)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            return mergeMessages(targetMessages, newMessages);
        }
    }

    private List<Message> receiveNewMessages(
            ReceiveMessageRequest.Builder requestBuilder, int queueProcessingTime, Instant start) {
        final var remainingTime = Math.max(0, queueProcessingTime - elapsedTime(start));

        return this.sqsClient
                .receiveMessage(
                        requestBuilder
                                .visibilityTimeout(getVisibilityTimeout(remainingTime))
                                .build())
                .messages();
    }

    private int getQueueSize(String queueUrl) {
        return Integer.parseInt(
                this.sqsClient
                        .getQueueAttributes(
                                GetQueueAttributesRequest.builder()
                                        .queueUrl(queueUrl)
                                        .attributeNames(APPROXIMATE_NUMBER_OF_MESSAGES)
                                        .build())
                        .attributes()
                        .get(APPROXIMATE_NUMBER_OF_MESSAGES));
    }

    private Map<Boolean, List<Message>> sortMessages(
            List<Message> messages, Map<String, String> filters) {
        return messages.stream()
                .collect(Collectors.partitioningBy(message -> messageMatches(message, filters)));
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
                                        .queueUrl(Objects.requireNonNull(queueUrl))
                                        .entries(deleteMessageBatchRequest(batch))
                                        .build()));
    }

    private void resetMessageVisibility(String queueUrl, Map<String, Message> messages) {
        changeMessageVisibility(queueUrl, 0, messages);
    }

    private Instant changeMessageVisibility(
            String queueUrl, int visibilityTimeout, Map<String, Message> messages) {
        final var start = Instant.now();
        batchOperation(
                List.copyOf(messages.values()),
                batch ->
                        this.sqsClient.changeMessageVisibilityBatch(
                                ChangeMessageVisibilityBatchRequest.builder()
                                        .queueUrl(queueUrl)
                                        .entries(
                                                changeMessageVisibilityBatchRequest(
                                                        batch, visibilityTimeout))
                                        .build()));
        return start;
    }

    private JsonNode parseJson(String json) {
        try {
            return this.objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static int getVisibilityTimeout(long remainingTime) {
        return Math.round(remainingTime) + SQS_WAIT_TIME;
    }

    private static int getQueueProcessingTime(int queueSize) {
        return (int) Math.round(Math.max(10, queueSize / 100d) * 2);
    }

    private static long elapsedTime(Instant start) {
        return Duration.between(start, Instant.now()).get(ChronoUnit.SECONDS);
    }

    private static boolean queueProcessed(int initialQueueSize, int receivedMessageCount) {
        return Math.abs((double) initialQueueSize / receivedMessageCount - 1) < 0.1;
    }

    private static boolean newMessagesReceived(
            HashSet<String> receivedMessageIds, HashSet<String> seenMessageIds) {
        return !seenMessageIds.containsAll(receivedMessageIds);
    }

    private static Set<String> mergeMessages(
            Map<String, Message> messages, Collection<Message> newMessages) {
        final var messagesById = messagesById(newMessages);
        messages.putAll(messagesById(newMessages));
        return messagesById.keySet();
    }

    private static Map<String, Message> messagesById(Collection<Message> messages) {
        return messages.stream()
                .collect(toMap(Message::messageId, identity(), (first, second) -> second));
    }

    private static void batchOperation(List<Message> messages, Consumer<List<Message>> operation) {
        split(messages, 10).forEach(operation);
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
            changeMessageVisibilityBatchRequest(List<Message> messages, int visibilityTimeout) {
        return messages.stream()
                .map(
                        message ->
                                ChangeMessageVisibilityBatchRequestEntry.builder()
                                        .id(message.messageId())
                                        .receiptHandle(message.receiptHandle())
                                        .visibilityTimeout(visibilityTimeout)
                                        .build())
                .collect(Collectors.toList());
    }
}
