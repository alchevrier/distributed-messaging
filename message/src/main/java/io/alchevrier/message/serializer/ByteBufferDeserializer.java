package io.alchevrier.message.serializer;

import io.alchevrier.message.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static io.alchevrier.message.serializer.MessageType.*;

public class ByteBufferDeserializer {
    public ConsumeRequest deserializeConsumeRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != CONSUME_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to ConsumeRequest as it is of type: " + type);
        }

        var topicLength = buffer.getInt();
        var topicBytes = new byte[topicLength];
        buffer.get(topicBytes);

        var partition = buffer.getInt();
        var startingOffset = buffer.getLong();
        var batchSize = buffer.getInt();

        return new ConsumeRequest(new Topic(new String(topicBytes)), partition, startingOffset, batchSize);
    }

    public ConsumeResponse deserializeConsumeResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != CONSUME_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to ConsumeRequest as it is of type: " + type);
        }

        var isSuccess = buffer.get() == 1;
        if (!isSuccess) {
            var errorLength = buffer.getInt();
            var errorBytes = new byte[errorLength];
            buffer.get(errorBytes);

            return new ConsumeResponse(null, null, new String(errorBytes));
        }

        var nextOffset = buffer.getLong();
        var messageCount = buffer.getInt();
        var messages = new ArrayList<Message>(messageCount);

        while (messageCount > 0) {
            var msgLength = buffer.getInt();
            var offset = buffer.getLong();
            var msgBytes = new byte[msgLength];
            buffer.get(msgBytes);

            messages.add(new Message(offset, msgBytes));

            messageCount--;
        }

        return new ConsumeResponse(messages, nextOffset, null);
    }

    public ProduceRequest deserializeProduceRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != PRODUCE_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to ProduceRequest as it is of type: " + type);
        }

        var topicLength = buffer.getInt();
        var topicBytes = new byte[topicLength];
        buffer.get(topicBytes);

        var keyLength = buffer.getInt();
        var keyBytes = new byte[keyLength];
        buffer.get(keyBytes);

        var dataLength = buffer.getInt();
        var dataBytes = new byte[dataLength];
        buffer.get(dataBytes);

        return new ProduceRequest(
                new Topic(new String(topicBytes)),
                keyBytes.length == 0 ? null : new String(keyBytes),
                dataBytes
        );
    }

    public ProduceResponse deserializeProduceResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != PRODUCE_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to ProduceRequest as it is of type: " + type);
        }

        if (buffer.get() == 1) {
            var offset = buffer.getLong();
            var partition = buffer.getInt();
            return new ProduceResponse(partition, offset, null);
        }

        var errorLength = buffer.getInt();
        var errorBytes = new byte[errorLength];
        buffer.get(errorBytes);
        var partition = buffer.getInt();
        return new ProduceResponse(partition, null, new String(errorBytes));
    }

    public FlushRequest deserializeFlushRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != FLUSH_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to ProduceRequest as it is of type: " + type);
        }

        return new FlushRequest();
    }

    public FlushResponse deserializeFlushResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != FLUSH_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to ProduceRequest as it is of type: " + type);
        }

        if (buffer.get() == 1) {
            return new FlushResponse(null);
        }

        var errorLength = buffer.getInt();
        var errorBytes = new byte[errorLength];
        buffer.get(errorBytes);

        return new FlushResponse(new String(errorBytes));
    }
}
