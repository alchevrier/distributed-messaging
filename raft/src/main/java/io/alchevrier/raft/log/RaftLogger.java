package io.alchevrier.raft.log;

import io.alchevrier.raft.RaftLogEntry;

public sealed interface RaftLogger permits FileChannelRaftLogger, InMemoryRaftLogger {
    long append(int length, long index, long term, byte[] data);
    RaftLogEntry getEntryAt(long position);
    void deleteFrom(long position);
}
