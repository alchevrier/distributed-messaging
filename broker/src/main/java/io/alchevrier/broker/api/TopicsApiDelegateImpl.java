package io.alchevrier.broker.api;

import io.alchevrier.broker.model.ConsumeResponse;
import io.alchevrier.broker.model.Message;
import io.alchevrier.broker.model.ProduceRequest;
import io.alchevrier.broker.model.ProduceResponse;
import io.alchevrier.logstorageengine.LogManager;
import io.alchevrier.message.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

@Component
public class TopicsApiDelegateImpl implements TopicsApiDelegate {

    private final LogManager logManager;

    public TopicsApiDelegateImpl(@Autowired LogManager logManager) {
        this.logManager = logManager;
    }

    @Override
    public ResponseEntity<ConsumeResponse> consume(String topic, Long offset, Long batchSize) {
        var consumedMessage = new LinkedList<Message>();

        for (var i = offset; i < batchSize + offset; i++) {
            try {
                var newMessage = new Message();
                newMessage.setOffset(i);
                var data = logManager.read(new Topic(topic), i);
                newMessage.setData(data);

                consumedMessage.add(newMessage);
            } catch (RuntimeException ex) {
                // breaking the loop meaning no over messages past the previous offset
                break;
            }
        }

        var result = new ConsumeResponse();
        result.setMessages(consumedMessage);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<ProduceResponse> produce(String topic, ProduceRequest produceRequest) {
        var offset = logManager.append(new Topic(topic), produceRequest.getData());
        var result = new ProduceResponse();
        result.setOffset(offset);
        return ResponseEntity.ok(result);
    }
}
