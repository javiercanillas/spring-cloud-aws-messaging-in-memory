package io.github.javiercanillas.amazonws.services.sqs;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Future;

@ExtendWith(MockitoExtension.class)
class ConsumeMethodHolderTest {

    @Mock
    Message<String> message;
    @Mock
    MessageHeaders messageHeaders;

    private Bean bean;

    @BeforeEach
    void before() {
        this.bean = new Bean(false);
        Mockito.doReturn(messageHeaders).when(message).getHeaders();
    }


    @Test
    void invokeNotMatchingArguments() {
        final ConsumeMethodHolder consumeMethodHolder = new ConsumeMethodHolder(bean, getBeanConsume1(),
                SqsMessageDeletionPolicy.NO_REDRIVE);

        Assertions.assertTrue(consumeMethodHolder.invoke(message));
        Assertions.assertNotEquals(message, bean.message);
    }

    @Test
    void invokeMatchingArguments01() {
        final ConsumeMethodHolder consumeMethodHolder = new ConsumeMethodHolder(bean, getBeanConsume1(),
                SqsMessageDeletionPolicy.NO_REDRIVE);

        Mockito.lenient().doReturn(true).when(messageHeaders).containsKey("senderId");
        Mockito.lenient().doReturn("senderIdCustom").when(messageHeaders).get("senderId");
        Mockito.lenient().doReturn("payload").when(message).getPayload();
        Assertions.assertTrue(consumeMethodHolder.invoke(message));
        Assertions.assertEquals(message, bean.message);
        Assertions.assertEquals("payload", bean.payload);
        Assertions.assertEquals(messageHeaders, bean.headers);
        Assertions.assertEquals("senderIdCustom", bean.senderId);
        Assertions.assertNull(bean.optional);
        Assertions.assertNotNull(bean.ack);
    }

    @Test
    void invokeThrowingException() {
        final ConsumeMethodHolder consumeMethodHolder = new ConsumeMethodHolder(bean, getBeanConsume2(),
                SqsMessageDeletionPolicy.NO_REDRIVE);

        Assertions.assertThrows(NotImplementedException.class, () -> consumeMethodHolder.invoke(message));
    }

    @Test
    void invokeMatchingArgumentsWithDoAcknowledge() {
        this.bean = new Bean(true);
        final ConsumeMethodHolder consumeMethodHolder = new ConsumeMethodHolder(bean, getBeanConsume1(),
                SqsMessageDeletionPolicy.NEVER);

        Mockito.lenient().when(messageHeaders.containsKey("senderId")).thenReturn(true);
        Mockito.lenient().when(messageHeaders.get("senderId")).thenReturn("senderIdCustom");
        Mockito.lenient().when(messageHeaders.containsKey("optional")).thenReturn(true);
        Mockito.lenient().when(messageHeaders.get("optional")).thenReturn("optionalValue");
        Mockito.lenient().when(message.getPayload()).thenReturn("payload");
        Assertions.assertTrue(consumeMethodHolder.invoke(message));
        Assertions.assertEquals(message, bean.message);
        Assertions.assertEquals("payload", bean.payload);
        Assertions.assertEquals(messageHeaders, bean.headers);
        Assertions.assertEquals("senderIdCustom", bean.senderId);
        Assertions.assertEquals("optionalValue", bean.optional);
        Assertions.assertNotNull(bean.ack);
    }

    @Test
    void invokeMatchingArgumentsWithoutDoAcknowledge() {
        final ConsumeMethodHolder consumeMethodHolder = new ConsumeMethodHolder(bean, getBeanConsume1(),
                SqsMessageDeletionPolicy.NEVER);

        Mockito.lenient().when(messageHeaders.containsKey("senderId")).thenReturn(true);
        Mockito.lenient().when(messageHeaders.get("senderId")).thenReturn("senderIdCustom");
        Mockito.lenient().when(messageHeaders.containsKey("optional")).thenReturn(true);
        Mockito.lenient().when(messageHeaders.get("optional")).thenReturn("optionalValue");
        Mockito.lenient().when(message.getPayload()).thenReturn("payload");
        Assertions.assertFalse(consumeMethodHolder.invoke(message));
        Assertions.assertEquals(message, bean.message);
        Assertions.assertEquals("payload", bean.payload);
        Assertions.assertEquals(messageHeaders, bean.headers);
        Assertions.assertEquals("senderIdCustom", bean.senderId);
        Assertions.assertEquals("optionalValue", bean.optional);
        Assertions.assertNotNull(bean.ack);
    }

    private Method getBeanConsume1() {
        return Arrays.stream(Bean.class.getMethods())
                .filter(method -> method.getName().equalsIgnoreCase("consume01"))
                .findFirst().orElseThrow();
    }

    private Method getBeanConsume2() {
        return Arrays.stream(Bean.class.getMethods())
                .filter(method -> method.getName().equalsIgnoreCase("consume02"))
                .findFirst().orElseThrow();
    }

    private static class Bean {

        private final boolean doAcknowledge;
        public Message<String> message;
        public String payload;
        public Map<String,Object> headers;
        public String senderId;
        public String optional;
        public Acknowledgment ack;

        public Bean(final boolean doAcknowledge) {
            this.doAcknowledge = doAcknowledge;
        }

        @SqsListener("consume1")
        public void consume01(final Message<String>  message,
                              @Payload final String payload,
                              @Headers final Map<String,Object> headers,
                              @Header("senderId") final String senderId,
                              @Header(name = "optional", required = false) final String optional,
                              Acknowledgment ack) {
            this.message = message;
            this.payload = payload;
            this.headers = headers;
            this.senderId = senderId;
            this.optional = optional;
            this.ack = ack;
            if (this.doAcknowledge) {
                ack.acknowledge();
            }
        }

        @SqsListener("consume2")
        public void consume02(final Pair invalid) {
            // do nothing
        }
    }

    private class TestPayload {
        public final String content;
        TestPayload(final String content) {
            this.content = content;
        }

    }

}