package io.alchevrier.consumer

import io.alchevrier.message.ConsumeResponse
import io.alchevrier.message.Message
import io.alchevrier.message.Topic
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class MessageConsumerTest extends Specification {

    MessageConsumerClient mockClient
    MessageConsumer objectUnderTest

    def setup() {
        mockClient = Mock(MessageConsumerClient)
        objectUnderTest = new DefaultMessageConsumer(mockClient)
    }

    def "when http error happened then should throw an exception to be handled by the consumer"() {
        when:
            objectUnderTest.consume(new Topic("test"), 10, 100)
        then:
            1 * mockClient.consume("test", 10, 100) >> ResponseEntity.badRequest()

            thrown RuntimeException
    }

    def "when attempting to read from a topic at an offset that has no message then should return empty response"() {
        given:
            def sentResponse = new ConsumeResponse(Collections.emptyList(), 10, null)
        when:
            def result = objectUnderTest.consume(new Topic("test"), 10, 100)
        then:
            1 * mockClient.consume("test", 10, 100) >> ResponseEntity.ok(sentResponse)

            result.messages().isEmpty()
            result.nextOffset() == 10
    }

    def "reading for a topic at a given offset and batchSize with messages"() {
        given:
            def sentResponse = new ConsumeResponse(List.of(new Message(10, "Hello".getBytes())), 11, null)
        when:
            def result = objectUnderTest.consume(new Topic("test"), 10, 1)
        then:
            1 * mockClient.consume("test", 10, 1) >> ResponseEntity.ok(sentResponse)

            result.messages().size() == 1
            result.messages()[0].offset() == 10
            new String(result.messages()[0].data()) == "Hello"
            result.nextOffset() == 11
    }
}
