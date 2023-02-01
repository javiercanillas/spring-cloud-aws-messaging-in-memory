package io.github.javiercanillas.amazonws.services.sqs;

import io.awspring.cloud.messaging.core.QueueMessageChannel;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.GenericMessage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InMemoryQueueMessagingTemplate extends QueueMessagingTemplate implements DisposableBean {

    private static final String DEFAULT = "__DEFAULT__";

    private final Map<String, SqsManager> instances;

    private final InMemoryAwsSqsClient client;

    public InMemoryQueueMessagingTemplate(final InMemoryAwsSqsClient amazonSqs) {
        super(amazonSqs);
        this.instances = new ConcurrentHashMap<>();
        var manager = new SqsManager(new SqsInstance(), false, amazonSqs);
        this.client = amazonSqs;
        this.instances.put(DEFAULT, manager);
    }

    @Override
    public void destroy() {
        final var sqsManagerList = new ArrayList<SqsManager>(this.instances.values());
        this.instances.clear();
        sqsManagerList.forEach(SqsManager::stop);
    }

    public boolean register(final Object bean, final Method method, final Set<String> queueNames,
                            final SqsMessageDeletionPolicy deletionPolicy) {
        final var clazz = bean.getClass();
        log.info("Registering {}.{} for queues: {} with deletationPolicy: {}", clazz.getSimpleName(), method,
                queueNames, deletionPolicy);


        queueNames.forEach(queueName -> {
            var sqsManager = this.instances.get(queueName);
            if (sqsManager == null) {
                sqsManager = new SqsManager(new SqsInstance(), true, this.client);
                sqsManager.start();
                this.instances.put(queueName, sqsManager);
            }
            sqsManager.addListener(bean, method, deletionPolicy);
        });
        return true;
    }

    public boolean unregister(final Object bean, final Method method, final Set<String> queueNames) {
        final var clazz = bean.getClass();
        log.info("Unregistering {}.{} for queues: {}", clazz.getSimpleName(), method,
                queueNames);
        queueNames.forEach(queueName -> {
            final var sqsManager = this.instances.get(queueName);
            if (sqsManager != null) {
                sqsManager.removeListener(bean, method);
            }
        });
        return true;
    }

    @Override
    public Message<?> receive() {
        return this.instances.get(DEFAULT).receive();
    }

    @Override
    public Message<?> receive(final String destinationName) {
        final var sqsManager = this.instances.get(destinationName);
        if (sqsManager != null) {
            return sqsManager.receive();
        } else {
            throw new MessagingException("Couldn't find resource by name: " + destinationName);
        }
    }

    @Override
    public Message<?> receive(final QueueMessageChannel destination) {
        return destination.receive();
    }

    @Override
    public <T> T receiveAndConvert(final Class<T> targetClass) {
        return convert(targetClass, this.receive());
    }

    @Override
    public <T> T receiveAndConvert(final String destinationName, final Class<T> targetClass) {
        return convert(targetClass, this.receive(destinationName));
    }

    @Override
    public <T> T receiveAndConvert(final QueueMessageChannel destination, final Class<T> targetClass) {
        return convert(targetClass, destination.receive());
    }

    @Override
    public void convertAndSend(final Object payload) {
        this.send(new GenericMessage<>(payload));
    }

    @Override
    public <T> void convertAndSend(final String destinationName, final T payload) {
        this.convertAndSend(destinationName, payload, (Map<String, Object>) null);
    }

    @Override
    public <T> void convertAndSend(final String destinationName, final T payload, final Map<String, Object> headers) {
        GenericMessage<Object> genericMessage;
        var id = UUID.randomUUID().toString();
        Map<String, Object> newHeaders = new HashMap<>();
        if (headers != null) {
            newHeaders.putAll(headers);
        }
        newHeaders.put("id", id);
        newHeaders.put("ReceiptHandle", id);
        genericMessage = new GenericMessage<>(payload, newHeaders);

        this.send(destinationName, genericMessage);
    }

    @Override
    public void convertAndSend(final QueueMessageChannel destination, final Object payload) {
        destination.send(new GenericMessage<>(payload));
    }

    @Override
    public void convertAndSend(final Object payload, final MessagePostProcessor postProcessor) {
        // To simplify, we will do nothing with the postProcessor
        this.convertAndSend(payload);
    }

    @Override
    public void convertAndSend(final QueueMessageChannel destination, final Object payload,
                               final Map<String, Object> headers) {
        destination.send(new GenericMessage<>(payload, headers));
    }

    @Override
    public <T> void convertAndSend(final String destinationName, final T payload,
                                   final MessagePostProcessor postProcessor) {
        // To simplify, we will do nothing with the postProcessor
        this.convertAndSend(destinationName, payload);
    }

    @Override
    public <T> void convertAndSend(final String destinationName, final T payload, final Map<String, Object> headers,
                                   final MessagePostProcessor postProcessor) {
        // To simplify, we will do nothing with the postProcessor
        this.convertAndSend(destinationName, payload, headers);
    }

    @Override
    public void convertAndSend(final QueueMessageChannel destination, final Object payload,
                               final MessagePostProcessor postProcessor) {
        // To simplify, we will do nothing with the postProcessor
        this.convertAndSend(destination, payload);
    }

    @Override
    public void convertAndSend(final QueueMessageChannel destination, final Object payload,
                               final Map<String, Object> headers,
                               final MessagePostProcessor postProcessor) {
        // To simplify, we will do nothing with the postProcessor
        this.convertAndSend(destination, payload, headers);
    }

    @Override
    public void send(final Message<?> message) {
        this.instances.get(DEFAULT).send(message);
    }

    @Override
    public void send(final String destinationName, final Message<?> message) {
        var sqsManager = this.instances.computeIfAbsent(destinationName, k -> {
            var newInstance = new SqsManager(new SqsInstance(), false, this.client);
            newInstance.start();
            return newInstance;
        });
        sqsManager.send(message);
    }

    @Override
    public void send(final QueueMessageChannel destination, final Message<?> message) {
        destination.send(message);
    }

    private <T> T convert(final Class<T> targetClass, final Message<?> received) {
        final var optionalPayload = Optional.ofNullable(received)
                .map(Message::getPayload);
        if (optionalPayload.isPresent()) {
            return optionalPayload
                    .filter(targetClass::isInstance)
                    .map(targetClass::cast)
                    .orElseThrow(() -> new MessagingException("Cannot convert "
                            + optionalPayload.get().getClass()
                            + " to "
                            + targetClass.getClassLoader()));
        } else {
            return null;
        }
    }
}
