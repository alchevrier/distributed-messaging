package io.alchevrier.broker.endpoint

import io.alchevrier.broker.service.TopicsService
import io.alchevrier.message.ConsumeResponse
import io.alchevrier.message.Message
import io.alchevrier.message.ProduceRequest
import io.alchevrier.message.ProduceResponse
import io.alchevrier.message.Topic
import org.springframework.http.HttpStatusCode
import spock.lang.Specification

class TopicsApiEndpointTest extends Specification {
    TopicsService topicsService
    TopicsApiEndpoint objectUnderTest

    def setup() {
        topicsService = Mock(TopicsService)
        objectUnderTest = new TopicsApiEndpoint(topicsService)
    }

    def "given a base offset, a topic name and a batch size of one then should provide consume response of size one"() {
        when: "calling the delegate to read the value"
            def result = objectUnderTest.consume("hello", 0, 1)
        then: "should have one message"
            result.statusCode == HttpStatusCode.valueOf(200)
            result.body.messages().size() == 1
            result.body.messages()[0].offset() == 0
            new String(result.body.messages()[0].data()) == "Hello World"

            1 * topicsService.consume("hello", 0, 1) >> new ConsumeResponse(
                    List.of(new Message(0, "Hello World".getBytes())), 1, null
            )
    }

    def "given a base offset, a topic name and a batch size of three then should provide consume response of size three"() {
        when: "calling the delegate to read the value"
            def result = objectUnderTest.consume("hello", 0, 3)
        then: "should have three messages"
            result.statusCode == HttpStatusCode.valueOf(200)
            result.body.messages().size() == 3
            result.body.messages()[0].offset() == 0
            new String(result.body.messages()[0].data()) == "Hello World"
            result.body.messages()[1].offset() == 1
            new String(result.body.messages()[1].data()) == "Second Message"
            result.body.messages()[2].offset() == 2
            new String(result.body.messages()[2].data()) == "Hey Pa!"

            1 * topicsService.consume("hello", 0, 3) >> new ConsumeResponse(
                    List.of(
                            new Message(0, "Hello World".getBytes()),
                            new Message(1, "Second Message".getBytes()),
                            new Message(2, "Hey Pa!".getBytes())
                    ), 3, null
            )
    }

    def "appending to the log should provide the offset it was written on"() {
        when: "calling the delegate to append a value"
            def produceRequest = new ProduceRequest(new Topic("hello"), "Hello".getBytes())

            def result = objectUnderTest.produce("hello", produceRequest)

        then: "should have the offset at which the message was written"
            result.statusCode == HttpStatusCode.valueOf(200)
            result.body.offset() == 100

            1 * topicsService.produce("hello", "Hello".getBytes()) >> new ProduceResponse(100, null)
    }
}
