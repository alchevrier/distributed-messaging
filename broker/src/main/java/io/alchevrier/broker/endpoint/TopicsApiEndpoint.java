package io.alchevrier.broker.endpoint;

import io.alchevrier.logstorageengine.LogManager;
import io.alchevrier.message.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;

@RestController
public class TopicsApiEndpoint {

    private final LogManager logManager;

    public TopicsApiEndpoint(@Autowired LogManager logManager) {
        this.logManager = logManager;
    }

    @GetMapping("/topics/{topic}/consume")
    public ResponseEntity<ConsumeResponse> consume(
            @PathVariable("topic") String topic,
            @RequestParam("offset") Long offset,
            @RequestParam("batchSize") Long batchSize
    ) {
        var consumedMessage = new LinkedList<Message>();

        var lastOffset = 0L;
        for (var i = offset; i < batchSize + offset; i++) {
            try {
                var data = logManager.read(new Topic(topic), i);
                consumedMessage.add(new Message(i, data));
                lastOffset = i;
            } catch (RuntimeException ex) {
                // breaking the loop meaning no over messages past the previous offset
                break;
            }
        }

        var result = new ConsumeResponse(consumedMessage, lastOffset + 1);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/topics/{topic}/produce")
    public ResponseEntity<ProduceResponse> produce(
            @PathVariable("topic") String topic,
            @RequestBody ProduceRequest produceRequest
    ) {
        var topicObj = new Topic(topic);
        var offset = logManager.append(topicObj, produceRequest.data());
        return ResponseEntity.ok(new ProduceResponse(offset, topicObj, null));
    }
}
