package io.alchevrier.broker.service

import io.alchevrier.logstorageengine.LogManager
import io.alchevrier.message.Topic
import spock.lang.Specification

class TopicsServiceTest extends Specification {
    LogManager logManager
    TopicsService objectUnderTest
    Topic testTopic

    def setup() {
        logManager = Mock(LogManager)
        objectUnderTest = new TopicsService(logManager)
        testTopic = new Topic("hello")
    }

    def "given a base offset, a topic name and a batch size of one then should provide consume response of size one"() {
        when: "calling the delegate to read the value"
            def result = objectUnderTest.consume("hello", 0, 1)
        then: "should have one message"
            result.messages().size() == 1
            result.messages()[0].offset() == 0
            new String(result.messages()[0].data()) == "Hello World"
            !result.error

            1 * logManager.read(testTopic, 0) >> "Hello World".getBytes()
    }

    def "given a base offset, a topic name and a batch size of three then should provide consume response of size three"() {
        when: "calling the delegate to read the value"
            def result = objectUnderTest.consume("hello", 0, 3)
        then: "should have three messages"
            result.messages().size() == 3
            result.messages()[0].offset() == 0
            new String(result.messages()[0].data()) == "Hello World"
            result.messages()[1].offset() == 1
            new String(result.messages()[1].data()) == "Second Message"
            result.messages()[2].offset() == 2
            new String(result.messages()[2].data()) == "Hey Pa!"
            !result.error

            1 * logManager.read(testTopic, 0) >> "Hello World".getBytes()
            1 * logManager.read(testTopic, 1) >> "Second Message".getBytes()
            1 * logManager.read(testTopic, 2) >> "Hey Pa!".getBytes()
    }

    def "appending to the log should provide the offset it was written on"() {
        when: "calling the delegate to append a value"
            def result = objectUnderTest.produce("hello", "Hello".getBytes())

        then: "should have the offset at which the message was written"
            result.offset() == 100
            result.error() == null

            1 * logManager.append(testTopic, "Hello".getBytes()) >> 100
    }
}
