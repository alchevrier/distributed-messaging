package io.alchevrier.consumer;

import io.alchevrier.message.ConsumeRequest;
import io.alchevrier.message.ConsumeResponse;
import io.alchevrier.message.Topic;
import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.tcpclient.TcpClient;

import java.io.IOException;

public class TcpMessageConsumer implements MessageConsumer {

    private final TcpClient client;
    private final ByteBufferSerializer serializer;
    private final ByteBufferDeserializer deserializer;

    public TcpMessageConsumer(
            TcpClient client,
            ByteBufferSerializer serializer,
            ByteBufferDeserializer deserializer
    ) {
        this.client = client;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public ConsumeResponse consume(Topic topic, int partition, long startOffset, int batchSize) {
        try {
            return client.forwardToServer(
                    new ConsumeRequest(topic, partition, startOffset, batchSize),
                    serializer::serialize,
                    deserializer::deserializeConsumeResponse
            );
        } catch (IOException e) {
            return new ConsumeResponse(null, null, e.getMessage());
        }
    }
}
