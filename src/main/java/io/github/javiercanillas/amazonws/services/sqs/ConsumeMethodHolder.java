package io.github.javiercanillas.amazonws.services.sqs;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
public class ConsumeMethodHolder {
    private final Object bean;
    private final Method method;
    private final SqsMessageDeletionPolicy deletionPolicy;

    public ConsumeMethodHolder(final Object bean, final Method method, final SqsMessageDeletionPolicy deletionPolicy) {
        this.bean = bean;
        this.method = method;
        this.deletionPolicy = deletionPolicy;
    }

    /**
     *
     * @param message the content to be invoked with
     * @return if message should be requeued or not
     */
    public boolean invoke(final Message<?> message) {
        Objects.requireNonNull(message, "content must not be null");
        final var ack = new InnerAcknowledgment();
        var invocationParameters = this.completeInvocationParameters(message, ack);

        if (!invocationParameters.isEmpty()) {
            var errored = false;
            try {
                this.method.invoke(this.bean, invocationParameters.toArray());
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Error invoking method.", e);
                errored = true;
            }

            if (this.deletionPolicy.equals(SqsMessageDeletionPolicy.ALWAYS)) {
                return false;
            } else if (this.deletionPolicy.equals(SqsMessageDeletionPolicy.NEVER)) {
                return ack.isAcknowledge();
            } else {
                return !errored;
            }
        } else {
            return true;
        }
    }

    private List<Object> completeInvocationParameters(final Message<?> message, final InnerAcknowledgment ack) {
        var matchThisInvocation = true;
        var invocationParameters = new ArrayList<>(this.method.getParameterCount());
        for (var parameter : method.getParameters()) {
            matchThisInvocation = addInvocationParameter(message, ack, parameter, invocationParameters);
            if (!matchThisInvocation) {
                break;
            }
        }
        if (matchThisInvocation) {
            return invocationParameters;
        } else {
            return Collections.emptyList();
        }
    }

    private boolean addInvocationParameter(final Message<?> message, final InnerAcknowledgment ack,
                                           final Parameter parameter,
                                           final ArrayList<Object> invocationParameters) {
        final var messageHeaders = message.getHeaders();
        final var parameterType = parameter.getType();
        if (parameterType == Message.class) {
            invocationParameters.add(message);
        } else if ((parameterType.isAssignableFrom(Map.class) && parameter.isAnnotationPresent(Headers.class))) {
            invocationParameters.add(messageHeaders);
        } else if (parameterType.isAssignableFrom(MessageHeaderAccessor.class)) {
            invocationParameters.add(MessageHeaderAccessor.getAccessor(message));
        } else if (parameterType.isAssignableFrom(Acknowledgment.class)) {
            invocationParameters.add(ack);
        } else if (parameter.isAnnotationPresent(Payload.class)) {
            invocationParameters.add(message.getPayload());
        } else if (parameter.isAnnotationPresent(Header.class)) {
            final var headerAnnotation = parameter.getAnnotation(Header.class);
            if (messageHeaders.containsKey(headerAnnotation.name())) {
                invocationParameters.add(messageHeaders.get(headerAnnotation.name()));
            } else if (messageHeaders.containsKey(headerAnnotation.value())) {
                invocationParameters.add(messageHeaders.get(headerAnnotation.value()));
            } else if (!headerAnnotation.required()) {
                invocationParameters.add(null);
            } else {
                return false;
            }
        } else {
            throw new NotImplementedException("Cannot handle parameter " + parameter.getName() + " of method "
                    + method.getName() + " from class " + this.bean.getClass().getSimpleName());
        }
        return true;
    }

    static class InnerAcknowledgment implements Acknowledgment {

        private boolean acknowledge = false;

        @Override
        public Future<?> acknowledge() {
            this.acknowledge = true;
            var resp = new CompletableFuture<>();
            resp.complete(new Object());
            return resp;
        }

        public boolean isAcknowledge() {
            return this.acknowledge;
        }
    }
}
