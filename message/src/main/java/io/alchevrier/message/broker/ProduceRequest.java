package io.alchevrier.message.broker;

import io.alchevrier.message.Topic;

public record ProduceRequest(Topic topic, String key, byte[] data) {
}
