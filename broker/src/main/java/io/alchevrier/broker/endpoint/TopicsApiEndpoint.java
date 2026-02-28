package io.alchevrier.broker.endpoint;

import io.alchevrier.broker.service.TopicsService;
import io.alchevrier.message.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TopicsApiEndpoint {

    private final TopicsService topicsService;

    public TopicsApiEndpoint(@Autowired TopicsService topicsService) {
        this.topicsService = topicsService;
    }

    @GetMapping("/topics/{topic}/consume")
    public ResponseEntity<ConsumeResponse> consume(
            @PathVariable("topic") String topic,
            @RequestParam("partition") Integer partition,
            @RequestParam("offset") Long offset,
            @RequestParam("batchSize") Integer batchSize
    ) {
        return ResponseEntity.ok(topicsService.consume(topic, partition, offset, batchSize));
    }

    @PostMapping("/topics/{topic}/produce")
    public ResponseEntity<ProduceResponse> produce(
            @PathVariable("topic") String topic,
            @RequestBody ProduceRequest produceRequest
    ) {
        return ResponseEntity.ok(topicsService.produce(topic, produceRequest.key(), produceRequest.data()));
    }
}
