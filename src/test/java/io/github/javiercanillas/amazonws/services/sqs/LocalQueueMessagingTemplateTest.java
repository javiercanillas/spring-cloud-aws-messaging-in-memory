package io.github.javiercanillas.amazonws.services.sqs;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.messaging.core.QueueMessageChannel;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class LocalQueueMessagingTemplateTest {

    Bean bean;

    private InMemoryQueueMessagingTemplate template;

    @Mock
    private AmazonSQSAsync amazonSqs;

    @Mock
    private Message<Object> message;

    @Mock
    private MessageHeaders messageHeaders;

    @Mock
    private QueueMessageChannel queueMessageChannel;

    @BeforeEach
    void setup() {
        Mockito.lenient().doReturn(messageHeaders).when(message).getHeaders();
        this.bean = new Bean();
        this.template = new InMemoryQueueMessagingTemplate(new InMemoryAwsSqsClient());
    }

    @AfterEach
    void cleanse() {
        this.template.destroy();
    }

    @Test
    void registerUnregister() {
        Assertions.assertTrue(this.template.unregister(this.bean, getBeanConsume1(), Set.of("consume1")));

        Assertions.assertTrue(this.template.register(this.bean, getBeanConsume1(), Set.of("consume1"), SqsMessageDeletionPolicy.NO_REDRIVE));
        Assertions.assertTrue(this.template.unregister(this.bean, getBeanConsume1(), Set.of("consume1")));

        Assertions.assertTrue(this.template.register(this.bean, getBeanConsume1(), Set.of("consume1"), SqsMessageDeletionPolicy.NO_REDRIVE));
        Assertions.assertTrue(this.template.register(this.bean, getBeanConsume1(), Set.of("consume1"), SqsMessageDeletionPolicy.NO_REDRIVE));
    }

    @Test
    void receiveNoQueueFound() {
        Assertions.assertThrows(MessagingException.class, () -> this.template.receive("unknown"));
    }

    @Test
    void sendAndReceive() {
        var id= UUID.randomUUID();
        Mockito.doReturn(id).when(messageHeaders).getId();
        this.template.send(message);
        Assertions.assertEquals(message, this.template.receive());

        Mockito.doReturn(message).when(queueMessageChannel).receive();
        this.template.send(queueMessageChannel, message);
        Assertions.assertEquals(message, this.template.receive(queueMessageChannel));
        Mockito.verify(queueMessageChannel, Mockito.times(1)).send(message);
        Mockito.verify(queueMessageChannel, Mockito.times(1)).receive();

        String queueName = "queueTest";
        this.template.send(queueName, message);
        Assertions.assertEquals(message, this.template.receive(queueName));
    }

    @Test
    void convertAndSendAndReceivedAndConvertWithChannel() {
        final TestPayload payload = new TestPayload("payload");

        Mockito.doReturn(payload).when(message).getPayload();
        Mockito.doReturn(message).when(queueMessageChannel).receive();
        this.template.convertAndSend(queueMessageChannel, payload);
        Assertions.assertEquals(payload, this.template.receiveAndConvert(queueMessageChannel, TestPayload.class));
        Mockito.verify(queueMessageChannel, Mockito.times(1)).send(Mockito.any());
        Mockito.verify(queueMessageChannel, Mockito.times(1)).receive();

        Mockito.reset(queueMessageChannel);

        Mockito.doReturn(payload).when(message).getPayload();
        Mockito.doReturn(message).when(queueMessageChannel).receive();
        this.template.convertAndSend(queueMessageChannel, payload, Map.of("sender", "senderValue"));
        Assertions.assertEquals(payload, this.template.receiveAndConvert(queueMessageChannel, TestPayload.class));
        Mockito.verify(queueMessageChannel, Mockito.times(1)).send(Mockito.any());
        Mockito.verify(queueMessageChannel, Mockito.times(1)).receive();

        Mockito.reset(queueMessageChannel);

        Mockito.doReturn(payload).when(message).getPayload();
        Mockito.doReturn(message).when(queueMessageChannel).receive();
        this.template.convertAndSend(queueMessageChannel, payload, (MessagePostProcessor) null);
        Assertions.assertEquals(payload, this.template.receiveAndConvert(queueMessageChannel, TestPayload.class));
        Mockito.verify(queueMessageChannel, Mockito.times(1)).send(Mockito.any());
        Mockito.verify(queueMessageChannel, Mockito.times(1)).receive();

        Mockito.reset(queueMessageChannel);

        Mockito.doReturn(payload).when(message).getPayload();
        Mockito.doReturn(message).when(queueMessageChannel).receive();
        this.template.convertAndSend(queueMessageChannel, payload, Map.of("sender", "senderValue"), null);
        Assertions.assertEquals(payload, this.template.receiveAndConvert(queueMessageChannel, TestPayload.class));
        Mockito.verify(queueMessageChannel, Mockito.times(1)).send(Mockito.any());
        Mockito.verify(queueMessageChannel, Mockito.times(1)).receive();
    }

    @Test
    void convertAndSendAndReceivedAndConvert() {
        final TestPayload payload = new TestPayload("payload");

        this.template.convertAndSend(payload);
        Assertions.assertEquals(payload, this.template.receiveAndConvert(TestPayload.class));

        this.template.convertAndSend("testQueue", payload);
        Assertions.assertEquals(payload, this.template.receiveAndConvert("testQueue", TestPayload.class));

        this.template.convertAndSend(payload, null);
        Assertions.assertEquals(payload, this.template.receiveAndConvert(TestPayload.class));

        this.template.convertAndSend("testQueue", payload, (MessagePostProcessor) null);
        Assertions.assertEquals(payload, this.template.receiveAndConvert("testQueue", TestPayload.class));

        this.template.convertAndSend("testQueue", payload, Map.of("sender", "senderValue"));
        Assertions.assertEquals(payload, this.template.receiveAndConvert("testQueue", TestPayload.class));

        this.template.convertAndSend("testQueue", payload, Map.of("sender", "senderValue"), null);
        Assertions.assertEquals(payload, this.template.receiveAndConvert("testQueue", TestPayload.class));
    }

    private Method getBeanConsume1() {
        return Arrays.stream(Bean.class.getMethods())
                .filter(method -> method.getName().equalsIgnoreCase("consume01"))
                .findFirst().orElseThrow();
    }

    private static class Bean {

        public Message<String> message;

        @SqsListener("consume1")
        public void consume01(final Message<String> message) {
            this.message = message;
        }
    }

    private class TestPayload {

        public final String content;

        TestPayload(final String content) {
            this.content = content;
        }

    }
}