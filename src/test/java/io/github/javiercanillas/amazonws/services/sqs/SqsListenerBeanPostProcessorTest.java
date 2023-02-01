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
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

@ExtendWith(MockitoExtension.class)
class SqsListenerBeanPostProcessorTest {
    @Mock
    private InMemoryQueueMessagingTemplate localQueueMessagingTemplate;

    private StringValueResolver stringValueResolver = s -> s;

    private SqsListenerBeanPostProcessor sqsListenerBeanPostProcessor;

    @BeforeEach
    void before() {
        this.sqsListenerBeanPostProcessor = new SqsListenerBeanPostProcessor(localQueueMessagingTemplate);
        this.sqsListenerBeanPostProcessor.setEmbeddedValueResolver(this.stringValueResolver);
    }

    @Test
    void postProcessBeforeDestruction() {
        Bean bean = new Bean();
        Mockito.doReturn(Boolean.TRUE).when(localQueueMessagingTemplate).unregister(Mockito.eq(bean),
                Mockito.eq(getBeanConsume1()), Mockito.anySet());
        this.sqsListenerBeanPostProcessor.postProcessBeforeDestruction(bean, "myBean");
        Mockito.verify(localQueueMessagingTemplate, Mockito.times(1)).unregister(Mockito.eq(bean),
                Mockito.eq(getBeanConsume1()), Mockito.anySet());
    }

    @Test
    void requiresDestruction() {
        Assertions.assertTrue(this.sqsListenerBeanPostProcessor.requiresDestruction(new Bean()));
    }

    @Test
    void postProcessBeforeInitialization() {
        Bean bean = new Bean();
        Assertions.assertEquals(bean, this.sqsListenerBeanPostProcessor.postProcessBeforeInitialization(bean, "myBean"));
    }

    @Test
    void postProcessAfterInitialization() {
        Bean bean = new Bean();
        Mockito.doReturn(Boolean.TRUE).when(localQueueMessagingTemplate).register(Mockito.eq(bean),
                Mockito.eq(getBeanConsume1()), Mockito.anySet(), Mockito.any(SqsMessageDeletionPolicy.class));
        Assertions.assertEquals(bean, this.sqsListenerBeanPostProcessor.postProcessAfterInitialization(bean, "myBean"));
        Mockito.verify(localQueueMessagingTemplate, Mockito.times(1)).register(Mockito.eq(bean),
                Mockito.eq(getBeanConsume1()), Mockito.anySet(), Mockito.any(SqsMessageDeletionPolicy.class));
    }

    @Test
    void postProcessAfterInitializationNullBean() {
        Bean bean = null;
        Assertions.assertNull(this.sqsListenerBeanPostProcessor.postProcessAfterInitialization(bean, "myBean"));
        Mockito.verify(localQueueMessagingTemplate, Mockito.never()).register(Mockito.eq(bean),
                Mockito.eq(getBeanConsume1()), Mockito.anySet(), Mockito.any(SqsMessageDeletionPolicy.class));
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
        public void consume01(final Message<String>  message) {
            this.message = message;
            this.countDownLatch.countDown();
        }
    }
}