package io.github.javiercanillas.amazonws.services.sqs;

import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class SqsListenerBeanPostProcessor implements DestructionAwareBeanPostProcessor, EmbeddedValueResolverAware {
    private final InMemoryQueueMessagingTemplate localQueueMessagingTemplate;
    private StringValueResolver stringValueResolver;

    public SqsListenerBeanPostProcessor(final InMemoryQueueMessagingTemplate localQueueMessagingTemplate) {
        this.localQueueMessagingTemplate = localQueueMessagingTemplate;
    }

    @Override
    public void postProcessBeforeDestruction(final Object bean, final String beanName) {
        this.process(bean, beanName, this::unregister);
    }

    @Override
    public boolean requiresDestruction(final Object bean) {
        return true;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        this.process(bean, beanName, this::register);
        return bean;
    }

    private boolean unregister(final Object bean, final Method method, final SqsListener annotation) {
        return this.localQueueMessagingTemplate.unregister(bean,
                method,
                Arrays.stream(annotation.value())
                        .map(this.stringValueResolver::resolveStringValue)
                        .collect(Collectors.toSet()));
    }

    private boolean register(final Object bean, final Method method, final SqsListener annotation) {
        return this.localQueueMessagingTemplate.register(bean,
                method,
                Arrays.stream(annotation.value())
                        .map(this.stringValueResolver::resolveStringValue)
                        .filter(Objects::nonNull)
                        .map(value -> Arrays.asList(value.split(",")))
                        .flatMap(List::stream)
                        .map(String::trim)
                        .collect(Collectors.toSet()),
                annotation.deletionPolicy());
    }

    @FunctionalInterface
    private interface TriPredicate<T, U, H> {
        boolean test(T t, U u, H h);
    }

    private void process(final Object bean, final String beanName, final TriPredicate<Object, Method, SqsListener> applyFunction) {
        if (bean != null) {
            var proxyClass = AopUtils.getTargetClass(bean);
            Arrays.stream(proxyClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(SqsListener.class))
                    .forEach(method -> {
                        var sqsAnnotation = method.getAnnotation(SqsListener.class);

                        if (!applyFunction.test(bean, method, sqsAnnotation)) {
                            log.error("Couldn't apply method {} of bean {}", method, bean);
                        }
                    });
        } else {
          log.warn("Skipping bean '{}' since object is null", beanName);
        }
    }

    @Override
    public void setEmbeddedValueResolver(final StringValueResolver resolver) {
        this.stringValueResolver = resolver;
    }
}
