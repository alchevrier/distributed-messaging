package io.alchevrier.broker

import io.alchevrier.broker.api.TopicsApiDelegate
import io.alchevrier.broker.api.TopicsApiDelegateImpl
import io.alchevrier.broker.model.ProduceRequest
import io.alchevrier.logstorageengine.LogManager
import io.alchevrier.message.Topic
import org.springframework.http.HttpStatusCode
import spock.lang.Specification

class TopicsApiDelegateTest extends Specification {
    LogManager mockManager
    Topic testTopic
    TopicsApiDelegate objectUnderTest

    def setup() {
        mockManager = Mock(LogManager)
        objectUnderTest = new TopicsApiDelegateImpl(mockManager)
        testTopic = new Topic("hello")
    }

    def "given a base offset, a topic name and a batch size of one then should provide consume response of size one"() {
        when: "calling the delegate to read the value"
            def result = objectUnderTest.consume("hello", 0, 1)
        then: "should have one message"
            result.statusCode == HttpStatusCode.valueOf(200)
            result.body.messages.size() == 1
            result.body.messages[0].offset == 0
            new String(result.body.messages[0].data) == "Hello World"

            1 * mockManager.read(testTopic, 0) >> "Hello World".getBytes()
    }

    def "given a base offset, a topic name and a batch size of three then should provide consume response of size three"() {
        when: "calling the delegate to read the value"
            def result = objectUnderTest.consume("hello", 0, 3)
        then: "should have three messages"
            result.statusCode == HttpStatusCode.valueOf(200)
            result.body.messages.size() == 3
            result.body.messages[0].offset == 0
            new String(result.body.messages[0].data) == "Hello World"
            result.body.messages[1].offset == 1
            new String(result.body.messages[1].data) == "Second Message"
            result.body.messages[2].offset == 2
            new String(result.body.messages[2].data) == "Hey Pa!"

            1 * mockManager.read(testTopic, 0) >> "Hello World".getBytes()
            1 * mockManager.read(testTopic, 1) >> "Second Message".getBytes()
            1 * mockManager.read(testTopic, 2) >> "Hey Pa!".getBytes()
    }

    def "appending to the log should provide the offset it was written on"() {
        when: "calling the delegate to append a value"
            def produceRequest = new ProduceRequest()
            produceRequest.setData("Hello".getBytes())

            def result = objectUnderTest.produce("hello", produceRequest)

        then: "should have the offset at which the message was written"
            result.statusCode == HttpStatusCode.valueOf(200)
            result.body.offset == 100

            1 * mockManager.append(testTopic, "Hello".getBytes()) >> 100
    }
}
