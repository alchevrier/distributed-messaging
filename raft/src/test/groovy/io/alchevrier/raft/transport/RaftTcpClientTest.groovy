package io.alchevrier.raft.transport

import io.alchevrier.message.raft.AppendEntriesRequest
import io.alchevrier.message.raft.AppendEntriesResponse
import io.alchevrier.message.raft.RequestVoteRequest
import io.alchevrier.message.raft.RequestVoteResponse
import io.alchevrier.message.serializer.ByteBufferDeserializer
import io.alchevrier.message.serializer.ByteBufferSerializer
import io.alchevrier.raft.RaftPeer
import io.alchevrier.tcpclient.TcpClient
import spock.lang.Specification

import java.util.function.BiConsumer

class RaftTcpClientTest extends Specification {

    TcpClient mockClient
    Map<Integer, TcpClient> clients
    ByteBufferSerializer mockSerializer
    ByteBufferDeserializer mockDeserializer
    BiConsumer<AppendEntriesResponse, Integer> mockHandler
    RaftTcpClient objectUnderTest

    RaftPeer testPeer

    def setup() {
        this.mockClient = Mock(TcpClient)
        clients = Map.of(1, mockClient)
        mockSerializer = Mock(ByteBufferSerializer)
        mockDeserializer = Mock(ByteBufferDeserializer)
        mockHandler = Mock(BiConsumer)
        objectUnderTest = new RaftTcpClient(clients, mockSerializer, mockDeserializer, mockHandler)

        testPeer = new RaftPeer(1, "localhost", 9009)
    }

    def "request for vote failing due to node connection lost"() {
        given: "a request vote request"
            def mockRequest = new RequestVoteRequest(1L, 1, 2L, 2L)
        when: "forwarding to tcp server"
            def result = objectUnderTest.requestVote(testPeer, mockRequest)
        then: "should return null due to node connection lost"
            1 * mockClient.forwardToServer(mockRequest, _, _) >> { throw new IOException() }
            result == null
    }

    def "request for vote success"() {
        given: "a request vote request"
            def mockRequest = new RequestVoteRequest(1L, 1, 2L, 2L)
            def mockResponse = new RequestVoteResponse(true, 1L)
        when: "forwarding to tcp server"
            def result = objectUnderTest.requestVote(testPeer, mockRequest)
        then: "should return a valid response"
            1 * mockClient.forwardToServer(mockRequest, _, _) >> mockResponse
            result == mockResponse
    }

    def "append entries failed failing due to node connection lost"() {
        given: "a append entries request"
            def mockRequest = new AppendEntriesRequest(1L, 1, 2l, 3L, 4L, new byte[0][])
        when: "forwarding to tcp server"
            objectUnderTest.appendEntries(testPeer, mockRequest)
        then: "should not have forwarded the answer to raft node"
            1 * mockClient.forwardToServer(mockRequest, _, _) >> { throw new IOException() }
            0 * mockHandler.accept(_, _)
    }

    def "append entries failed failing due to node connection lost"() {
        given: "a append entries request"
            def mockRequest = new AppendEntriesRequest(1L, 1, 2l, 3L, 4L, new byte[0][])
            def mockResponse = new AppendEntriesResponse(true, 1L, null, null)
        when: "forwarding to tcp server"
            objectUnderTest.appendEntries(testPeer, mockRequest)
        then: "should not have forwarded the answer to raft node"
            1 * mockClient.forwardToServer(mockRequest, _, _) >> mockResponse
            1 * mockHandler.accept(mockResponse, 1)
    }
}
