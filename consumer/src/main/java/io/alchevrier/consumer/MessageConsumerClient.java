package io.alchevrier.consumer;

import io.alchevrier.message.ConsumeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface MessageConsumerClient {
    @GetExchange("/topics/{topic}/consume")
    ResponseEntity<ConsumeResponse> consume(
            @PathVariable String topic,
            @RequestParam Long offset,
            @RequestParam int batchSize,
            @RequestParam int partition
    );
}
