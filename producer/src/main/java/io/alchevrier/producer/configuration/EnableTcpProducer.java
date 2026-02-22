package io.alchevrier.producer.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TcpProducerConfiguration.class)
public @interface EnableTcpProducer {
}
