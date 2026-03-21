package io.alchevrier.raft;

@FunctionalInterface
public interface LogScanFunction {
    boolean test(long index, RaftLogEntry entry);
}
