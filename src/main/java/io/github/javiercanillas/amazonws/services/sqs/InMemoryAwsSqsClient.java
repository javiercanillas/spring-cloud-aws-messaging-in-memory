package io.github.javiercanillas.amazonws.services.sqs;

import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityResult;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Instances of this class are used to simulate an AWS SQS client. Right now, this class only mimics the change of
 * a message visibility timeout since it is the only method required by the SQS solution.
 *
 */
@Getter
@Setter
@SuppressWarnings("deprecation")
public class InMemoryAwsSqsClient extends AmazonSQSAsyncClient {

    /**
     * This map contains the mapping between messages and the assigned timeouts.
     */
    private final ConcurrentHashMap<String, Integer> handles;

    /**
     * Default constructor.
     */
    public InMemoryAwsSqsClient() {
        this.handles = new ConcurrentHashMap<>();
    }

    /**
     * Changes the visibility timeout of a message.
     *
     * @param request this objet contains information required to change the timeout.
     *
     * @return an object that holds the result of this operation.
     */
    @Override
    public ChangeMessageVisibilityResult changeMessageVisibility(final ChangeMessageVisibilityRequest request) {
        var handle = request.getReceiptHandle();
        handles.put(handle, request.getVisibilityTimeout());
        return new ChangeMessageVisibilityResult();
    }

    /**
     * Getter.
     *
     * @param aKey the key of an assigned timeout.
     *
     * @return the timeout assigned to a message or null.
     */
    public Integer getHandle(final String aKey) {
        return this.handles.get(aKey);
    }
}
