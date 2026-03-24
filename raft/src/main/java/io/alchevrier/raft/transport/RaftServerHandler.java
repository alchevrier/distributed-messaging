package io.alchevrier.raft.transport;

import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.message.serializer.MessageType;
import io.alchevrier.raft.RaftNode;
import io.alchevrier.tcpserver.ServerHandler;

import java.nio.ByteBuffer;

public class RaftServerHandler implements ServerHandler {

    private final ByteBufferSerializer serializer;
    private final ByteBufferDeserializer deserializer;
    private final RaftNode raftNode;

    public RaftServerHandler(
            ByteBufferSerializer serializer,
            ByteBufferDeserializer deserializer,
            RaftNode raftNode
    ) {
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.raftNode = raftNode;
    }

    @Override
    public byte[] handle(byte[] message) {
        var buffer = ByteBuffer.wrap(message);
        var messageType = buffer.get();
        return switch (messageType) {
            case MessageType.REQUEST_VOTE_REQUEST -> handleRequestVoteRequest(message);
            case MessageType.APPEND_ENTRIES_REQUEST -> handleAppendEntriesRequest(message);
            default -> throw new IllegalArgumentException("Could not deserialize message type for type: " + messageType);
        };
    }

    private byte[] handleRequestVoteRequest(byte[] message) {
        var request = deserializer.deserializeRequestVoteRequest(message);
        var response = raftNode.handleRequestVote(request);
        return serializer.serialize(response);
    }

    private byte[] handleAppendEntriesRequest(byte[] message) {
        var request = deserializer.deserializeAppendEntriesRequest(message);
        var response = raftNode.handleAppendEntries(request);
        return serializer.serialize(response);
    }
}
