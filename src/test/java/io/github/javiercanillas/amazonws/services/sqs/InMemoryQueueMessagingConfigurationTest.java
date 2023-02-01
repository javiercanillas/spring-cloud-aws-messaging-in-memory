package io.github.javiercanillas.amazonws.services.sqs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class InMemoryQueueMessagingConfigurationTest {
    
    private InMemoryQueueMessagingConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new InMemoryQueueMessagingConfiguration();
    }

    @Test
    void queueMessagingTemplate() {
        var amazonSQSAsync = configuration.inMemoryAwsSqsClient();
        var localQueueMessagingTemplate = configuration.queueMessagingTemplate(amazonSQSAsync);
        assertNotNull(localQueueMessagingTemplate);
    }

    @Test
    void sqsListenerBeanPostProcessor() {
        var amazonSQSAsync = configuration.inMemoryAwsSqsClient();
        var localQueueMessagingTemplate = configuration.queueMessagingTemplate(amazonSQSAsync);
        var sqsListenerBeanPostProcessor = configuration.sqsListenerBeanPostProcessor(localQueueMessagingTemplate);
        assertNotNull(sqsListenerBeanPostProcessor);
    }

    @Test
    void queueMessageHandler() {
        assertNotNull(configuration.queueMessageHandler());
    }
}