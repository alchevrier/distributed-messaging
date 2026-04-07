package io.alchevrier.raft;

public record LogProperties(String indexerPath, long indexerSize, String loggerPath) {
}
