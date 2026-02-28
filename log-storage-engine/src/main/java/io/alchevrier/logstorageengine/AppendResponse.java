package io.alchevrier.logstorageengine;

public record AppendResponse(int partition, long offset) {
}
