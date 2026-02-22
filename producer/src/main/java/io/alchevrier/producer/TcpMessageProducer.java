package io.alchevrier.producer;

import io.alchevrier.message.ProduceRequest;
import io.alchevrier.message.ProduceResponse;
import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.tcpclient.TcpClient;

import java.io.IOException;

public class TcpMessageProducer implements MessageProducer {

    private final TcpClient client;
    private final ByteBufferSerializer serializer;
    private final ByteBufferDeserializer deserializer;

    public TcpMessageProducer(
            TcpClient client,
            ByteBufferSerializer serializer,
            ByteBufferDeserializer deserializer
    ) {
        this.client = client;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public ProduceResponse produce(ProduceRequest produceRequest) {
        try {
            return client.forwardToServer(produceRequest, serializer::serialize, deserializer::deserializeProduceResponse);
        } catch (IOException e) {
            return new ProduceResponse(null, e.getMessage());
        }
    }
}
