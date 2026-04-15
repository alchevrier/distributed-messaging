package io.alchevrier.broker.service;

import io.alchevrier.logstorageengine.LogManager;
import io.alchevrier.message.*;
import io.alchevrier.message.broker.ConsumeResponse;
import io.alchevrier.message.broker.ProduceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.LinkedList;

@Service
public class TopicsService {

    private final LogManager logManager;
    private final ByteBuffer readBuffer;

    public TopicsService(@Autowired LogManager logManager, @Value("${broker.read-buffer-size}") int readBufferSize) {
        this.logManager = logManager;
        this.readBuffer = ByteBuffer.allocate(readBufferSize);
    }

    public ConsumeResponse consume(String topic, int partition, Long offset, Integer batchSize) {
        var consumedMessage = new LinkedList<Message>();

        var lastOffset = 0L;
        for (var i = offset; i < batchSize + offset; i++) {
            try {
                readBuffer.clear();
                int bytesRead = logManager.read(new Topic(topic), partition, i, readBuffer);
                byte[] data = new byte[bytesRead];
                readBuffer.get(data);
                consumedMessage.add(new Message(i, data));
                lastOffset = i;
            } catch (RuntimeException ex) {
                // breaking the loop meaning no over messages past the previous offset
                break;
            }
        }

        return new ConsumeResponse(consumedMessage, lastOffset + 1, null);
    }

    public ProduceResponse produce(String topic, String key, byte[] data) {
        var response = logManager.append(new Topic(topic), key, data);
        return new ProduceResponse(
                response.partition(),
                response.offset(),
                null
        );
    }

    public void flush() {
        logManager.flush();
    }
}
