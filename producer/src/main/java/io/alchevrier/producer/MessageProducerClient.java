package io.alchevrier.producer;

import io.alchevrier.message.ProduceRequest;
import io.alchevrier.message.ProduceResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface MessageProducerClient {
    @PostExchange("/topics/{topic}/produce")
    ProduceResponse produce(
            @PathVariable String topic,
            @RequestBody ProduceRequest produceRequest
    );
}
