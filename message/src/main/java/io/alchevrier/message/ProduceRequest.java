package io.alchevrier.message;

public record ProduceRequest(Topic topic, byte[] data) {
}
