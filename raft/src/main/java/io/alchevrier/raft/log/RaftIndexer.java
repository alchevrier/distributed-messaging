package io.alchevrier.raft.log;

public sealed interface RaftIndexer permits MemoryMappedRaftIndexer, PassthroughRaftIndexer {
    long recover();
    void append(long index, long position);
    long getPosition(long index);
    void deleteFrom(long index);
}
