package io.alchevrier.broker.service

import io.alchevrier.logstorageengine.AppendResponse
import io.alchevrier.logstorageengine.LogManager
import io.alchevrier.message.Topic
import spock.lang.Specification

import java.nio.ByteBuffer

class TopicsServiceTest extends Specification {
    LogManager logManager
    TopicsService objectUnderTest
    Topic testTopic

    def setup() {
        logManager = Mock(LogManager)
        objectUnderTest = new TopicsService(logManager, 200)
        testTopic = new Topic("hello")
    }

    def "given a base offset, a topic name and a batch size of one then should provide consume response of size one"() {
        when: "calling the delegate to read the value"
            def result = objectUnderTest.consume("hello", 0, 0, 1)
        then: "should have one message"
            result.messages().size() == 1
            result.messages()[0].offset() == 0
            new String(result.messages()[0].data()) == "Hello World"
            !result.error

            1 * logManager.read(testTopic, 0, 0, _) >> { args ->
                def buf = "Hello World".getBytes()
                (args[3] as ByteBuffer).put(buf)
                (args[3] as ByteBuffer).flip()
                buf.length
            }
    }

    def "given a base offset, a topic name and a batch size of three then should provide consume response of size three"() {
        when: "calling the delegate to read the value"
            def result = objectUnderTest.consume("hello", 0, 0, 3)
        then: "should have three messages"
            result.messages().size() == 3
            result.messages()[0].offset() == 0
            new String(result.messages()[0].data()) == "Hello World"
            result.messages()[1].offset() == 1
            new String(result.messages()[1].data()) == "Second Message"
            result.messages()[2].offset() == 2
            new String(result.messages()[2].data()) == "Hey Pa!"
            !result.error

            1 * logManager.read(testTopic, 0, 0, _) >> { args ->
                def buf = "Hello World".getBytes()
                (args[3] as ByteBuffer).put(buf)
                (args[3] as ByteBuffer).flip()
                buf.length
            }
            1 * logManager.read(testTopic, 0, 1, _) >> { args ->
                def buf = "Second Message".getBytes()
                (args[3] as ByteBuffer).put(buf)
                (args[3] as ByteBuffer).flip()
                buf.length
            }
            1 * logManager.read(testTopic, 0, 2, _) >> { args ->
                def buf = "Hey Pa!".getBytes()
                (args[3] as ByteBuffer).put(buf)
                (args[3] as ByteBuffer).flip()
                buf.length
            }
    }

    def "appending to the log should provide the offset it was written on"() {
        when: "calling the delegate to append a value"
            def result = objectUnderTest.produce("hello", null, "Hello".getBytes())

        then: "should have the offset at which the message was written"
            result.partition() == 0
            result.offset() == 100
            result.error() == null

            1 * logManager.append(testTopic, null, "Hello".getBytes()) >> new AppendResponse(0, 100)
    }
}
