package io.alchevrier.broker.endpoint;

import io.alchevrier.broker.service.TopicsService;
import io.alchevrier.message.FlushResponse;
import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.tcpserver.ServerHandler;

import java.nio.ByteBuffer;

import static io.alchevrier.message.serializer.MessageType.*;

public class BrokerEndpointHandler implements ServerHandler {

    private final ByteBufferSerializer serializer;
    private final ByteBufferDeserializer deserializer;
    private final TopicsService topicsService;

    public BrokerEndpointHandler(
            ByteBufferSerializer serializer,
            ByteBufferDeserializer deserializer,
            TopicsService topicsService
    ) {
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.topicsService = topicsService;
    }

    @Override
    public byte[] handle(byte[] message) {
        var buffer = ByteBuffer.wrap(message);
        var messageType = buffer.get();
        return switch (messageType) {
            case CONSUME_REQUEST -> handleConsumeRequest(message);
            case PRODUCE_REQUEST -> handleProduceRequest(message);
            case FLUSH_REQUEST -> handleFlushRequest(message);
            default -> throw new IllegalArgumentException("Did not recognize messageType for type: " + messageType);
        };
    }

    private byte[] handleConsumeRequest(byte[] message) {
        var request = deserializer.deserializeConsumeRequest(message);
        var response = topicsService.consume(request.topic().name(), request.partition(), request.startingOffset(), request.batchSize());
        return serializer.serialize(response);
    }

    private byte[] handleProduceRequest(byte[] message) {
        var request = deserializer.deserializeProduceRequest(message);
        var response = topicsService.produce(request.topic().name(), request.key(), request.data());
        return serializer.serialize(response);
    }

    private byte[] handleFlushRequest(byte[] ignored) {
        try {
            topicsService.flush();
            return serializer.serialize(new FlushResponse(null));
        } catch (Exception e) {
            return serializer.serialize(new FlushResponse(e.getMessage()));
        }
    }
}
