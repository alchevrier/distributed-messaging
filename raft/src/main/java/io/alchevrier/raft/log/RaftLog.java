package io.alchevrier.raft.log;

import io.alchevrier.raft.LogScanFunction;
import io.alchevrier.raft.RaftLogEntry;

public sealed interface RaftLog permits InMemoryRaftLog, CompositeRaftLog {
    long append(long term, byte[] data);
    RaftLogEntry get(long index);
    long getLastIndex();
    long getLastTerm();
    long getTermAt(long index);
    void deleteFrom(long index);
    long scanFirst(long from, long to, LogScanFunction fn);
}
