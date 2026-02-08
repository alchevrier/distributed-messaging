package io.alchevrier.producer.integration

import io.alchevrier.producer.MessageProducer
import io.alchevrier.producer.MessageProducerClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

@SpringBootTest(
        classes = SmokeApplication.class,
        properties = "client.brokerUrl=http://mybroker:9001"
)
class SmokeClientTest extends Specification {
    @Autowired
    ApplicationContext context

    @Autowired
    MessageProducerClient producerClient

    @Autowired
    MessageProducer messageProducer

    def "should load full context without any missing bean or property error"() {
        expect:
            context != null
            producerClient != null
            messageProducer != null
    }
}
