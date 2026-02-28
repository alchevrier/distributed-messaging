package io.alchevrier.producer

import io.alchevrier.message.ProduceRequest
import io.alchevrier.message.ProduceResponse
import io.alchevrier.message.Topic
import spock.lang.Specification

class MessageProducerTest extends Specification {
    MessageProducerClient mockClient
    MessageProducer objectUnderTest

    def setup() {
        mockClient = Mock(MessageProducerClient)
        objectUnderTest = new DefaultMessageProducer(mockClient)
    }

    def "provided a producer response then should forward it to the client"() {
        given:
            def sentResponse = new ProduceResponse(0, 1, "this is an error")
            def produceRequest = new ProduceRequest(new Topic("hello"), null, "Hi".getBytes())
        when:
            def result = objectUnderTest.produce(produceRequest)
        then:
            1 * mockClient.produce("hello", produceRequest) >> sentResponse
            result == sentResponse
    }
}
