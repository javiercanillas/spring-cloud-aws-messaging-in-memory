package io.github.javiercanillas.amazonws.services.sqs;

import io.awspring.cloud.messaging.core.SqsMessageHeaders;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SqsManager {

    private final SqsInstance sqsInstance;

    private final Thread consumerThread;

    private final Map<Pair<Object, Method>, ConsumeMethodHolder> hookedConsumers;

    private final InMemoryAwsSqsClient client;

    public SqsManager(final SqsInstance sqsInstance, final boolean createConsumer, final InMemoryAwsSqsClient client) {
        Objects.requireNonNull(sqsInstance, "sqsInstance must not be null");
        this.sqsInstance = sqsInstance;
        this.hookedConsumers = new ConcurrentHashMap<>();
        this.client = client;
        if (createConsumer) {
            this.consumerThread = new Thread(this::consume);
        } else {
            this.consumerThread = null;
        }
    }

    @SuppressWarnings("java:S1452")
    public Message<?> receive() {
        return this.sqsInstance.poll();
    }

    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final var taken = this.sqsInstance.take();
                boolean removed = this.hookedConsumers.values().stream()
                        .map(consumer -> this.handledConsume(consumer, taken))
                        .reduce(Boolean::logicalOr)
                        .orElse(false);
                if (!removed) {
                    this.send(taken);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean handledConsume(final ConsumeMethodHolder consumeMethodHolder, final Message<?> taken) {
        try {
            return consumeMethodHolder.invoke(taken);
        } catch (RuntimeException e) {
            log.error("There was an error executing consumer {}", consumeMethodHolder, e);
            return false;
        }
    }

    public void start() {
        if (this.consumerThread != null) {
            this.consumerThread.start();
        }
    }

    public void stop() {
        log.trace("Stopping!!!!");
        if (this.consumerThread != null) {
            this.consumerThread.interrupt();
        }
    }

    public void addListener(final Object bean, final Method method,
                            final SqsMessageDeletionPolicy deletionPolicy) {
        Objects.requireNonNull(bean, "bean must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(deletionPolicy, "deletionPolicy must not be null");
        this.hookedConsumers.putIfAbsent(Pair.of(bean, method), new ConsumeMethodHolder(bean, method, deletionPolicy));
    }

    public void removeListener(final Object bean, final Method method) {
        Objects.requireNonNull(bean, "bean must not be null");
        Objects.requireNonNull(method, "method must not be null");
        this.hookedConsumers.remove(Pair.of(bean, method));
    }

    public void send(final Message<?> message) {
        Objects.requireNonNull(message, "message must not be null");
        var key = Optional.ofNullable(message.getHeaders().getId()).map(Objects::toString).orElse("");
        var value = this.client.getHandle(key);
        if (null != value) {
            this.client.getHandles().remove(key);
            this.sqsInstance.add(message, TimeUnit.SECONDS.toMillis(value.longValue()));
        } else {
            var delayValue = message.getHeaders().get(SqsMessageHeaders.SQS_DELAY_HEADER);
            if (delayValue instanceof Number) {
                this.sqsInstance.add(message, TimeUnit.SECONDS.toMillis(((Number) delayValue).longValue()));
            } else {
                this.sqsInstance.add(message);
            }
        }
    }


}
