package io.alchevrier.raft;

public interface RaftLog {
    long append(long term, byte[] data);
    RaftLogEntry get(long index);
    long getLastIndex();
    long getLastTerm();
    long getTermAt(long index);
    void deleteFrom(long index);
}
