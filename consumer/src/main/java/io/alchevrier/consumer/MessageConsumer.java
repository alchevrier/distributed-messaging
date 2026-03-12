package io.alchevrier.consumer;

import io.alchevrier.message.broker.ConsumeResponse;
import io.alchevrier.message.Topic;

public interface MessageConsumer {
    ConsumeResponse consume(Topic topic, int partition, long startOffset, int batchSize);
}
