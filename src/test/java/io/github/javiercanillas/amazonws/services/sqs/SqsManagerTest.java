package io.github.javiercanillas.amazonws.services.sqs;

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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class SqsManagerTest {

    @Mock
    Message<Object> message;

    @Mock
    private SqsInstance sqsInstance;

    @BeforeEach
    void before() {
    }

    @Test
    void manualActions() {
        final SqsManager sqsManager = new SqsManager(sqsInstance, false, new InMemoryAwsSqsClient());
        sqsManager.start();
        MessageHeaders messageHeaders = Mockito.mock(MessageHeaders.class);
        Mockito.doReturn(messageHeaders).when(message).getHeaders();
        Mockito.doReturn(message).when(sqsInstance).poll();
        sqsManager.send(message);
        Assertions.assertEquals(message, sqsManager.receive());
        sqsManager.stop();
        Mockito.verify(sqsInstance, Mockito.times(1)).add(message);
    }

    @Test
    void automaticActions() throws InterruptedException {
        MessageHeaders messageHeaders = Mockito.mock(MessageHeaders.class);
        Mockito.doReturn(messageHeaders).when(message).getHeaders();
        Mockito.doReturn(message).when(sqsInstance).take();
        final SqsManager sqsManager = new SqsManager(sqsInstance, true, new InMemoryAwsSqsClient());
        sqsManager.start();
        Bean myBean = new Bean();
        sqsManager.addListener(myBean, getBeanConsume1(), SqsMessageDeletionPolicy.NO_REDRIVE);
        sqsManager.send(message);
        myBean.countDownLatch.await(1, TimeUnit.SECONDS);
        Assertions.assertEquals(message, myBean.message);
        sqsManager.stop();
        Mockito.verify(sqsInstance, Mockito.atLeast(1)).add(message);

    }

    private Method getBeanConsume1() {
        return Arrays.stream(Bean.class.getMethods())
                .filter(method -> method.getName().equalsIgnoreCase("consume01"))
                .findFirst().orElseThrow();
    }

    private static class Bean {

        public CountDownLatch countDownLatch = new CountDownLatch(1);

        public Message<String> message;

        @SqsListener("consume1")
        public void consume01(final Message<String> message) {
            this.message = message;
            this.countDownLatch.countDown();
        }
    }
}