package io.github.javiercanillas.amazonws.services.sqs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class SqsInstanceTest {

    private SqsInstance instance;
    
    @Mock
    private Message msg01;
    @Mock
    private Message msg02;
    @Mock
    private Message delayedMessage;

    @BeforeEach
    void beforeEach() {
        this.instance = new SqsInstance();
    }
    
    @Test
    void delayedConsuming() throws InterruptedException {
        instance.add(delayedMessage, 1000L);
        instance.add(msg01);
        instance.add(msg02);

        Assertions.assertEquals(msg01, instance.take());
        Assertions.assertEquals(msg02, instance.take());
        Assertions.assertEquals(delayedMessage, instance.take());
    }
}