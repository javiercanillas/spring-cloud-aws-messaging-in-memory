package io.github.javiercanillas.amazonws.services.sqs;

import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalAwsSQSClientTest {

    @Test
    void testChangeMessageVisibility() {
        var local = new InMemoryAwsSqsClient();
        var request = new ChangeMessageVisibilityRequest();
        request.setReceiptHandle("handle_1");
        request.setVisibilityTimeout(Integer.valueOf(12));
        local.changeMessageVisibility(request);

        Assertions.assertEquals(1, local.getHandles().size());
        Assertions.assertTrue(local.getHandles().containsKey("handle_1"));
        Assertions.assertEquals(Integer.valueOf(12), local.getHandles().get("handle_1"));
    }

}
