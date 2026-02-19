package io.alchevrier.message.serializer

import io.alchevrier.message.ConsumeRequest
import io.alchevrier.message.ConsumeResponse
import io.alchevrier.message.FlushRequest
import io.alchevrier.message.FlushResponse
import io.alchevrier.message.Message
import io.alchevrier.message.ProduceRequest
import io.alchevrier.message.ProduceResponse
import io.alchevrier.message.Topic
import spock.lang.Specification

import java.util.stream.IntStream

class ByteBufferSerializerTest extends Specification {

    ByteBufferSerializer objectUnderTest
    ByteBufferDeserializer anotherObjectUnderTest

    def setup() {
        objectUnderTest = new ByteBufferSerializer()
        anotherObjectUnderTest = new ByteBufferDeserializer()
    }

    def "given a producer request then should serialize-deserialize as expected"() {
        given: "a producer request"
            def request = new ProduceRequest(new Topic("hello"), "Hello World".getBytes())
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeProduceRequest(toBytes)
        then: "should match the producer request"
            result.topic() == request.topic()
            new String(result.data()) == new String(request.data())
    }

    def "given a successful producer response then should serialize-deserialize as expected"() {
        given: "a producer response"
            def response = new ProduceResponse(8, null)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeProduceResponse(toBytes)
        then: "should match the producer response"
            response == result
    }

    def "given a failed producer response then should serialize-deserialize as expected"() {
        given: "a producer response"
            def response = new ProduceResponse(null, "Could not persist message")
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeProduceResponse(toBytes)
        then: "should match the producer response"
            response == result
    }

    def "given a consume request then should serialize-deserialize as expected"() {
        given: "a consume request"
            def request = new ConsumeRequest(new Topic("hello-topic"), 18, 1000)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeConsumeRequest(toBytes)
        then: "should match a consume request"
            request == result
    }

    def "given a failure consume response then serialize-deserialize as expected"() {
        given: "a failure consume response"
            def response = new ConsumeResponse(null, null, "Could not consume as expected")
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeConsumeResponse(toBytes)
        then: "should match the result"
            response == result
    }

    def "given a successful consume response then should serialize-deserialize as expected"() {
        given: "a successful consume response"
            def response = new ConsumeResponse(
                    IntStream.range(0, 10).mapToObj {new Message(it, "hello-world-${it}!".getBytes())  }.toList(),
                    10,
                    null
            )
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeConsumeResponse(toBytes)
        then: "should match the result"
            response.nextOffset() == result.nextOffset()
            response.error() == result.error()
            response.messages().size() == result.messages().size()
            for (def i = 0; i < response.messages().size(); i++) {
                response.messages().get(i).offset() == result.messages().get(i).offset()
                new String(response.messages().get(i).data()) == new String(result.messages().get(i).data())
            }
    }

    def "given a flush request then should serialize-deserialize as expected"() {
        given: "a flush request"
            def request = new FlushRequest()
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeFlushRequest(toBytes)
        then: "should match the result"
            request == result
    }

    def "given a failure flush response then should serialize-deserialize as expected"() {
        given: "a failure flush response"
            def response = new FlushResponse("Failed to flush")
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeFlushResponse(toBytes)
        then: "should match the result"
            response == result
    }

    def "given a successful flush response then should serialize-deserialize as expected"() {
        given: "a successful flush response"
            def response = new FlushResponse(null)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeFlushResponse(toBytes)
        then: "should match the result"
            response == result
    }
}
