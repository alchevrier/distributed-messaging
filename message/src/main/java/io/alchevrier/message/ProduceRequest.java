package io.alchevrier.message;

public record ProduceRequest(Topic topic, String key, byte[] data) {
}
