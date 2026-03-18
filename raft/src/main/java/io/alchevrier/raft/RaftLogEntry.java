package io.alchevrier.raft;

public record RaftLogEntry(long term, byte[] data) {
}
