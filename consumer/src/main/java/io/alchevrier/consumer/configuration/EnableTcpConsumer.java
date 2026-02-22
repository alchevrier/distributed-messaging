package io.alchevrier.consumer.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TcpConfiguration.class)
public @interface EnableTcpConsumer {
}
