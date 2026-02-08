package io.alchevrier.consumer.integration

import io.alchevrier.consumer.MessageConsumer
import io.alchevrier.consumer.MessageConsumerClient
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
    MessageConsumerClient consumerClient

    @Autowired
    MessageConsumer messageConsumer

    def "should load full context without any missing bean or property error"() {
        expect:
            context != null
            consumerClient != null
            messageConsumer != null
    }
}
