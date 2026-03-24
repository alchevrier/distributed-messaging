package io.alchevrier.raft.transport

import io.alchevrier.message.raft.AppendEntriesRequest
import io.alchevrier.message.raft.AppendEntriesResponse
import io.alchevrier.message.raft.RequestVoteRequest
import io.alchevrier.message.raft.RequestVoteResponse
import io.alchevrier.message.serializer.ByteBufferDeserializer
import io.alchevrier.message.serializer.ByteBufferSerializer
import io.alchevrier.message.serializer.MessageType
import io.alchevrier.raft.RaftNode
import spock.lang.Specification

class RaftServerHandlerTest extends Specification {

    ByteBufferSerializer mockSerializer = Mock(ByteBufferSerializer.class)
    ByteBufferDeserializer mockDeserializer = Mock(ByteBufferDeserializer.class)
    RaftNode mockRaftNode = Mock(RaftNode.class)
    RaftServerHandler objectUnderTest = new RaftServerHandler(mockSerializer, mockDeserializer, mockRaftNode)

    def "given a request vote request then should handle it accordingly"() {
        given: "a request vote request"
            def request = new byte[] { MessageType.REQUEST_VOTE_REQUEST }
            def mockRequest = new RequestVoteRequest(1L, 1, 2L, 2L)
            def mockResponse = new RequestVoteResponse(true, 1L)
            def mockByteResponse = "Hello".getBytes()
        when: "handling the request"
            def response = objectUnderTest.handle(request)
        then: "should forward to the raft node and send the response"
            1 * mockDeserializer.deserializeRequestVoteRequest(request) >> mockRequest
            1 * mockRaftNode.handleRequestVote(mockRequest) >> mockResponse
            1 * mockSerializer.serialize(mockResponse) >> mockByteResponse

            new String(response) == "Hello"
    }

    def "given a append entries request then should handle it accordingly"() {
        given: "a append entries request"
            def request = new byte[] { MessageType.APPEND_ENTRIES_REQUEST }
            def mockRequest = new AppendEntriesRequest(1L, 1, 2l, 3L, 4L, new byte[0][])
            def mockResponse = new AppendEntriesResponse(true, 1L, null, null)
            def mockByteResponse = "World".getBytes()
        when: "handling the request"
            def response = objectUnderTest.handle(request)
        then: "should forward to the raft node and send the response"
            1 * mockDeserializer.deserializeAppendEntriesRequest(request) >> mockRequest
            1 * mockRaftNode.handleAppendEntries(mockRequest) >> mockResponse
            1 * mockSerializer.serialize(mockResponse) >> mockByteResponse

            new String(response) == "World"
    }
}
