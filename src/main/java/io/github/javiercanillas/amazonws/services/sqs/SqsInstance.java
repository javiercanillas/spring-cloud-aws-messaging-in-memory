package io.github.javiercanillas.amazonws.services.sqs;

import org.springframework.messaging.Message;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class SqsInstance {
    public static final String CONTENT_MUST_NOT_BE_NULL = "content must not be null";
    private final DelayQueue<DelayedItem> internalQueue;

    public SqsInstance() {
        this.internalQueue = new DelayQueue<>();
    }

    public boolean add(final Message<?> content) {
        Objects.requireNonNull(content, CONTENT_MUST_NOT_BE_NULL);
        return this.add(content, 0L);
    }

    public boolean add(final Message<?> content, final long delayInMillis) {
        Objects.requireNonNull(content, CONTENT_MUST_NOT_BE_NULL);
        return this.internalQueue.add(new DelayedItem(content, delayInMillis));
    }

    @SuppressWarnings("java:S1452")
    public Message<?> take() throws InterruptedException {
        return this.internalQueue.take().getContent();
    }

    @SuppressWarnings("java:S1452")
    public Message<?> poll() {
        final var taken = this.internalQueue.poll();
        return Optional.ofNullable(taken).map(DelayedItem::getContent).orElse(null);
    }

    private static class DelayedItem implements Delayed {

        private final Message<?> content;
        private final long consumeOnTimeInMillis;

        DelayedItem(final Message<?> content, final long delayInSeconds) {
            Objects.requireNonNull(content, CONTENT_MUST_NOT_BE_NULL);
            this.content = content;
            this.consumeOnTimeInMillis = System.currentTimeMillis() + (delayInSeconds > 0 ? delayInSeconds : 0);
        }
        @Override
        public long getDelay(final TimeUnit unit) {
            return unit.convert(this.consumeOnTimeInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(final Delayed o) {
            Objects.requireNonNull(o, "o must not be null");
            var value = this.consumeOnTimeInMillis - ((DelayedItem) o).getConsumeOnTimeInMillis();
            if (value > 2147483647L) {
                return 2147483647;
            } else {
                return value < -2147483648L ? -2147483648 : (int)value;
            }
        }

        public long getConsumeOnTimeInMillis() {
            return consumeOnTimeInMillis;
        }

        @SuppressWarnings("java:S1452")
        public Message<?> getContent() {
            return content;
        }
    }
}
