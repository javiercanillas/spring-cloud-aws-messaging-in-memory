package io.github.javiercanillas.amazonws.services.sqs;

import io.awspring.cloud.messaging.listener.QueueMessageHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This configuration class presents an internal implementation of SQS. It will be enabled by default when this property
 * is set as false. This property should match with the one enabling it in `io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration`
 *
 * You should only import this configuration, it's harmless unless you turn off the property explicitly.
 */
@ConditionalOnProperty(value = "cloud.aws.sqs.enabled", havingValue = "false")
@Configuration(value = "Mocked SQS spring configuration")
public class InMemoryQueueMessagingConfiguration {

    @Bean
    public InMemoryQueueMessagingTemplate queueMessagingTemplate(final InMemoryAwsSqsClient amazonSqs) {
        return new InMemoryQueueMessagingTemplate(amazonSqs);
    }

    @Bean
    public SqsListenerBeanPostProcessor sqsListenerBeanPostProcessor(final InMemoryQueueMessagingTemplate queueMessagingTemplate) {
        return new SqsListenerBeanPostProcessor(queueMessagingTemplate);
    }

    @Bean
    public InMemoryAwsSqsClient inMemoryAwsSqsClient() {
        return new InMemoryAwsSqsClient();
    }

    @Bean
    public QueueMessageHandler queueMessageHandler() {
        return new QueueMessageHandler();
    }


}
