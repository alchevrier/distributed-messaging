package io.alchevrier.producer;

import io.alchevrier.message.broker.ProduceRequest;
import io.alchevrier.message.broker.ProduceResponse;

public interface MessageProducer {
    ProduceResponse produce(ProduceRequest produceRequest);
}
