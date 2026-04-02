package io.alchevrier.raft.transport

import io.alchevrier.message.raft.AckMode
import io.alchevrier.message.raft.AppendRequest
import io.alchevrier.message.raft.AppendResponse
import io.alchevrier.message.serializer.ByteBufferDeserializer
import io.alchevrier.message.serializer.ByteBufferSerializer
import io.alchevrier.message.serializer.MessageType
import io.alchevrier.raft.RaftNode
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class RaftBrokerHandlerTest extends Specification {

    ByteBufferSerializer mockSerializer = Mock(ByteBufferSerializer.class)
    ByteBufferDeserializer mockDeserializer = Mock(ByteBufferDeserializer.class)
    RaftNode mockRaftNode = Mock(RaftNode.class)
    RaftBrokerHandler objectUnderTest = new RaftBrokerHandler(mockDeserializer, mockSerializer, mockRaftNode, 100L)

    def "given a non recognized message type then should throw an erro"() {
        given: "a non recognized message type"
            def request = new byte[] { MessageType.APPEND_ENTRIES_REQUEST }
        when: "handling the request"
            objectUnderTest.handle(request)
        then: "should throw an error"
            thrown IllegalArgumentException
    }

    def "given an append request then should handle accordingly"() {
        given: "an append request"
            def request = new byte[] { MessageType.APPEND_REQUEST }
            def mockRequest = new AppendRequest("Hello", new byte[0][], AckMode.NONE)
            def mockResponse = new AppendResponse(true, null)
            def mockByteResponse = "Hello".getBytes()
        when: "handling the request"
            def response = objectUnderTest.handle(request)
        then: "should forward to the raft node and send the response"
            1 * mockDeserializer.deserializeAppendRequest(request) >> mockRequest
            1 * mockRaftNode.append(mockRequest) >> CompletableFuture.completedFuture(mockResponse)
            1 * mockSerializer.serialize(mockResponse) >> mockByteResponse

            new String(response) == "Hello"
    }
}
