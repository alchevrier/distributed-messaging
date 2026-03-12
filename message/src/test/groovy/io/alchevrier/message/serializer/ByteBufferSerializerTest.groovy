package io.alchevrier.message.serializer

import io.alchevrier.message.broker.ConsumeRequest
import io.alchevrier.message.broker.ConsumeResponse
import io.alchevrier.message.broker.FlushRequest
import io.alchevrier.message.broker.FlushResponse
import io.alchevrier.message.Message
import io.alchevrier.message.broker.ProduceRequest
import io.alchevrier.message.broker.ProduceResponse
import io.alchevrier.message.Topic
import io.alchevrier.message.raft.AppendEntriesRequest
import io.alchevrier.message.raft.AppendEntriesResponse
import io.alchevrier.message.raft.RequestVoteRequest
import io.alchevrier.message.raft.RequestVoteResponse
import spock.lang.Specification

import java.util.stream.IntStream

class ByteBufferSerializerTest extends Specification {

    ByteBufferSerializer objectUnderTest
    ByteBufferDeserializer anotherObjectUnderTest

    def setup() {
        objectUnderTest = new ByteBufferSerializer()
        anotherObjectUnderTest = new ByteBufferDeserializer()
    }

    def "given a producer request without key then should serialize-deserialize as expected"() {
        given: "a producer request"
            def request = new ProduceRequest(new Topic("hello"), null, "Hello World".getBytes())
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeProduceRequest(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match the producer request"
            result.topic() == request.topic()
            result.key() == null
            new String(result.data()) == new String(request.data())
    }

    def "given a producer request with key then should serialize-deserialize as expected"() {
        given: "a producer request"
            def request = new ProduceRequest(new Topic("hello"), "MyKey", "Hello World".getBytes())
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeProduceRequest(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match the producer request"
            result.topic() == request.topic()
            result.key() == "MyKey"
            new String(result.data()) == new String(request.data())
    }

    def "given a successful producer response then should serialize-deserialize as expected"() {
        given: "a producer response"
            def response = new ProduceResponse(1, 8, null)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeProduceResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match the producer response"
            response == result
    }

    def "given a failed producer response then should serialize-deserialize as expected"() {
        given: "a producer response"
            def response = new ProduceResponse(-1, null, "Could not persist message")
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeProduceResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match the producer response"
            response == result
    }

    def "given a consume request then should serialize-deserialize as expected"() {
        given: "a consume request"
            def request = new ConsumeRequest(new Topic("hello-topic"), 2, 18, 1000)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeConsumeRequest(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match a consume request"
            request == result
    }

    def "given a failure consume response then serialize-deserialize as expected"() {
        given: "a failure consume response"
            def response = new ConsumeResponse(null, null, "Could not consume as expected")
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeConsumeResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
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
            def result = anotherObjectUnderTest.deserializeConsumeResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
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
            def result = anotherObjectUnderTest.deserializeFlushRequest(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match the result"
            request == result
    }

    def "given a failure flush response then should serialize-deserialize as expected"() {
        given: "a failure flush response"
            def response = new FlushResponse("Failed to flush")
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeFlushResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match the result"
            response == result
    }

    def "given a successful flush response then should serialize-deserialize as expected"() {
        given: "a successful flush response"
            def response = new FlushResponse(null)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeFlushResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match the result"
            response == result
    }

    def "given a request vote request then should serialize-deserialize as expected"() {
        given: "a request vote request"
            def request = new RequestVoteRequest(3, 1, 100, 2)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeRequestVoteRequest(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match"
            result == request
    }

    def "given a successful request vote response then should serialize-deserialize as expected"() {
        given: "a request vote response"
            def response = new RequestVoteResponse(true, 2)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeRequestVoteResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match"
            result == response
    }

    def "given a non-successful request vote response then should serialize-deserialize as expected"() {
        given: "a request vote response"
            def response = new RequestVoteResponse(false, 2)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeRequestVoteResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match"
            result == response
    }

    def "given an append entries request then should serialize-deserialize as expected"() {
        given: "a append entries request"
            def request = new AppendEntriesRequest(
                    2,
                    1,
                    10,
                    9,
                    1,
                    new byte[][] { "Hello".getBytes(), "World".getBytes(), "This is my world".getBytes() }
            )
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeAppendEntriesRequest(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match"
            result.term() == request.term()
            result.leaderId() == request.leaderId()
            result.leaderCommitIndex() == request.leaderCommitIndex()
            result.prevLogIndex() == request.prevLogIndex()
            result.prevLogTerm() == request.prevLogTerm()
            result.entries().length == request.entries().length

            for (int i = 0; i < result.entries().length; i++) {
                new String(result.entries()[i]) == new String(request.entries()[i])
            }
    }

    def "given an heartbeat request then should serialize-deserialize as expected"() {
        given: "an heartbeat request"
            def request = new AppendEntriesRequest(
                    2,
                    1,
                    10,
                    9,
                    1,
                    new byte[][] { }
            )
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(request)
            def result = anotherObjectUnderTest.deserializeAppendEntriesRequest(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match"
            result.term() == request.term()
            result.leaderId() == request.leaderId()
            result.leaderCommitIndex() == request.leaderCommitIndex()
            result.prevLogIndex() == request.prevLogIndex()
            result.prevLogTerm() == request.prevLogTerm()
            result.entries().length == 0
            result.entries().length == request.entries().length
    }

    def "given a successful append entries response then should serialize-deserialize as expected"() {
        given: "a successful append entries response"
            def response = new AppendEntriesResponse(true, 1, null, null)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeAppendEntriesResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match"
            result == response
    }

    def "given a non-successful append entries response then should serialize-deserialize as expected"() {
        given: "a non-successful append entries response"
            def response = new AppendEntriesResponse(false, 1, 2, 10)
        when: "serializing-deserializing"
            def toBytes = objectUnderTest.serialize(response)
            def result = anotherObjectUnderTest.deserializeAppendEntriesResponse(
                    Arrays.copyOfRange(toBytes, 4, toBytes.length)
            )
        then: "should match"
            result == response
    }
}
