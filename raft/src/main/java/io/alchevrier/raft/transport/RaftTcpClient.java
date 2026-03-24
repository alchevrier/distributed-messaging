package io.alchevrier.raft.transport;

import io.alchevrier.message.raft.AppendEntriesRequest;
import io.alchevrier.message.raft.AppendEntriesResponse;
import io.alchevrier.message.raft.RequestVoteRequest;
import io.alchevrier.message.raft.RequestVoteResponse;
import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.raft.RaftClient;
import io.alchevrier.raft.RaftPeer;
import io.alchevrier.tcpclient.TcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;

public class RaftTcpClient implements RaftClient {

    private static final Logger logger = LoggerFactory.getLogger(RaftTcpClient.class);

    private final Map<Integer, TcpClient> clients;
    private final ByteBufferSerializer serializer;
    private final ByteBufferDeserializer deserializer;
    private final BiConsumer<AppendEntriesResponse, Integer> onAppendEntriesResponse;

    public RaftTcpClient(
            Map<Integer, TcpClient> clients,
            ByteBufferSerializer serializer,
            ByteBufferDeserializer deserializer,
            BiConsumer<AppendEntriesResponse, Integer> onAppendEntriesResponse
    ) {
        this.clients = clients;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.onAppendEntriesResponse = onAppendEntriesResponse;
    }

    @Override
    public RequestVoteResponse requestVote(RaftPeer peer, RequestVoteRequest request) {
        try {
            return clients.get(peer.nodeId()).forwardToServer(
                    request, serializer::serialize, deserializer::deserializeRequestVoteResponse
            );
        } catch (IOException e) {
            logger.warn("requestVote to peer {} failed: {}", peer.nodeId(), e.getMessage());
            return null;
        }
    }

    @Override
    public void appendEntries(RaftPeer peer, AppendEntriesRequest request) {
        try {
            var response = clients
                    .get(peer.nodeId())
                    .forwardToServer(request, serializer::serialize, deserializer::deserializeAppendEntriesResponse);
            onAppendEntriesResponse.accept(response, peer.nodeId());
        } catch (IOException e) {
            // expected during normal cluster operation (peer unreachable)
            logger.warn("appendEntries to peer {} failed: {}", peer.nodeId(), e.getMessage());
        }
    }
}
