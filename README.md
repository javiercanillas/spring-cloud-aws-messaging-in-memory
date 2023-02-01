# spring-cloud-aws-messaging-in-memory
Does it happen to you that your Spring-boot application uses an Amazon SQS service through [spring-cloud-aws-messaging](https://spring.io/projects/spring-cloud-aws) dependency
and you depend on an internet connection on your local or development environment? 
Well, this little dependency tries to supply the same behaviour as Amazon SQS by using in-memory Java Queues and DelayedQueues.

[![Java CI with Maven](https://github.com/javiercanillas/spring-cloud-aws-messaging-in-memory/actions/workflows/maven-build.yml/badge.svg)](https://github.com/javiercanillas/spring-cloud-aws-messaging-in-memory/actions/workflows/maven-build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=javiercanillas_spring-cloud-aws-messaging-in-memory&metric=alert_status)](https://sonarcloud.io/dashboard?id=javiercanillas_spring-cloud-aws-messaging-in-memory)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=javiercanillas_spring-cloud-aws-messaging-in-memory&metric=coverage)](https://sonarcloud.io/dashboard?id=javiercanillas_spring-cloud-aws-messaging-in-memory)
---
## How does it work?
Well, The entry point to send and receive message is `QueueMessagingTemplate`, so basically I provide my own version of it. 
After that, I needed to add funtionally for the auto-discovery of the annotated consumer methods and bind them to the internal
implementation of java Queues.

Sounded easy right? Take a look at the code, pull-reuests with improvements are welcomed!

**Note:** If you consider there is something missing, feel free to contact [me](https://github.com/javiercanillas/spring-cloud-aws-messaging-in-memory/issues).

## How to use
Simple, probably you have already added the [spring-cloud-aws-messaging](https://spring.io/projects/spring-cloud-aws) dependency, so you
only need to follow these simple steps:

- Add this dependency to your classpath (check [How to install](#how-to-install))
- After that remember to import its configuration like the following:
```java
import org.springframework.context.annotation.Import;

@Configuration()
@Import(io.github.javiercanillas.amazonws.services.sqs.InMemoryQueueMessagingTemplate)
public class LocalConfiguration {
    ...
}
```
- And turn into false the following property: 
```properties
cloud.aws.sqs.enabled=false
```
By doing so, your are turning-off the real SQS support.

## How to install
If you prefer to use maven central releases, you can find it [here](https://search.maven.org/artifact/io.github.javiercanillas/spring-cloud-aws-messaging-in-memory). Also, if you support [Jitpack.io](https://jitpack.io/) you can find it [here](https://jitpack.io/#javiercanillas/spring-cloud-aws-messaging-in-memory)
