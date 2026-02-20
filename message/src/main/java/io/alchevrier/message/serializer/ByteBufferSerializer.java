package io.alchevrier.message.serializer;

import io.alchevrier.message.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.alchevrier.message.serializer.MessageType.*;

public class ByteBufferSerializer {
    public byte[] serialize(ConsumeRequest consumeRequest) {
        var topicName = consumeRequest.topic().name().getBytes(StandardCharsets.UTF_8);
        var length = 4 + 1 + 4 + topicName.length * 4 + 4 + 8 + 4;

        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(CONSUME_REQUEST);
        buffer.putInt(topicName.length);
        buffer.put(topicName);
        buffer.putLong(consumeRequest.startingOffset());
        buffer.putInt(consumeRequest.batchSize());

        return buffer.array();
    }

    public byte[] serialize(ConsumeResponse consumeResponse) {
        if (consumeResponse.isError()) {
            var error = consumeResponse.error().getBytes(StandardCharsets.UTF_8);
            var length = 4 + 1 + 1 + 4 + error.length;

            var buffer = ByteBuffer.allocate(length);
            buffer.putInt(length);
            buffer.put(CONSUME_RESPONSE);
            buffer.put((byte) 0);
            buffer.putInt(error.length);
            buffer.put(error);

            return buffer.array();
        } else {
            var messageLength = consumeResponse.messages()
                    .stream()
                    .map(it -> 4 + 8 + it.data().length)
                    .reduce(Integer::sum)
                    .orElse(0);

            var length = 4 + 1 + 1 + 8 + 4 + messageLength;

            var buffer = ByteBuffer.allocate(length);
            buffer.putInt(length);
            buffer.put(CONSUME_RESPONSE);
            buffer.put((byte) 1);
            buffer.putLong(consumeResponse.nextOffset());
            buffer.putInt(consumeResponse.messages().size());

            for (int i = 0; i < consumeResponse.messages().size(); i++) {
                var message = consumeResponse.messages().get(i);
                buffer.putInt(message.data().length);
                buffer.putLong(message.offset());
                buffer.put(message.data());
            }

            return buffer.array();
        }
    }

    public byte[] serialize(ProduceRequest produceRequest) {
        var topicName = produceRequest.topic().name().getBytes(StandardCharsets.UTF_8);
        var length = 4 + 1 + 4 + topicName.length + 4 + produceRequest.data().length;

        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(PRODUCE_REQUEST);
        buffer.putInt(topicName.length);
        buffer.put(topicName);
        buffer.putInt(produceRequest.data().length);
        buffer.put(produceRequest.data());

        return buffer.array();
    }

    public byte[] serialize(ProduceResponse produceResponse) {
        var errorBytes = produceResponse.isError() ? produceResponse.error().getBytes(StandardCharsets.UTF_8) : new byte[0];
        var errorLength = produceResponse.isError() ? errorBytes.length : 0;
        var offsetLength = produceResponse.isError() ? 0 : 8;

        var length = 4 + 1 + 1 + errorLength + errorBytes.length + offsetLength;

        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(PRODUCE_RESPONSE);
        buffer.put((byte) (produceResponse.isError() ? 0 : 1));

        if (!produceResponse.isError()) {
            buffer.putLong(produceResponse.offset());
        } else {
            buffer.putInt(errorLength);
            buffer.put(produceResponse.error().getBytes(StandardCharsets.UTF_8));
        }

        return buffer.array();
    }

    public byte[] serialize(FlushRequest flushRequest) {
        var length = 4 + 1;
        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(FLUSH_REQUEST);
        return buffer.array();
    }

    public byte[] serialize(FlushResponse flushResponse) {
        if (flushResponse.isError()) {
            var errorArray = flushResponse.error().getBytes(StandardCharsets.UTF_8);
            var length = 4 + 1 + 1 + 4 + errorArray.length;
            var buffer = ByteBuffer.allocate(length);
            buffer.putInt(length);
            buffer.put(FLUSH_RESPONSE);
            buffer.put((byte) 0);
            buffer.putInt(errorArray.length);
            buffer.put(errorArray);
            return buffer.array();
        } else {
            var length = 4 + 1 + 1;
            var buffer = ByteBuffer.allocate(length);
            buffer.putInt(length);
            buffer.put(FLUSH_RESPONSE);
            buffer.put((byte) 1);
            return buffer.array();
        }
    }
}
