package io.alchevrier.producer;

import io.alchevrier.message.ProduceRequest;
import io.alchevrier.message.ProduceResponse;

public interface MessageProducer {
    ProduceResponse produce(ProduceRequest produceRequest);
}
