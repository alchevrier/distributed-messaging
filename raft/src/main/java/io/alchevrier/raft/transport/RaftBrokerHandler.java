package io.alchevrier.raft.transport;

import io.alchevrier.message.raft.AppendResponse;
import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.message.serializer.MessageType;
import io.alchevrier.raft.RaftNode;
import io.alchevrier.tcpserver.ServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class RaftBrokerHandler implements ServerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaftBrokerHandler.class);

    private final ByteBufferDeserializer deserializer;
    private final ByteBufferSerializer serializer;
    private final RaftNode raftNode;
    private final long timeout;

    public RaftBrokerHandler(
            ByteBufferDeserializer deserializer,
            ByteBufferSerializer serializer,
            RaftNode raftNode,
            long timeout
    ) {
        this.deserializer = deserializer;
        this.serializer = serializer;
        this.raftNode = raftNode;
        this.timeout = timeout;
    }

    @Override
    public byte[] handle(byte[] message) {
        var buffer = ByteBuffer.wrap(message);
        var messageType = buffer.get();
        return switch (messageType) {
            case MessageType.APPEND_REQUEST -> handleAppendRequest(message);
            default -> throw new IllegalArgumentException("Could not deserialize message type for type: " + messageType);
        };
    }

    private byte[] handleAppendRequest(byte[] message) {
        try {
            var request = deserializer.deserializeAppendRequest(message);
            var response = raftNode.append(request);
            return serializer.serialize(response.get(timeout, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            LOGGER.error("handleAppendRequest failed", e);
            return serializer.serialize(new AppendResponse(false, null));
        }
    }
}
