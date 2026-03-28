package io.alchevrier.raft.log;

import io.alchevrier.raft.LogScanFunction;
import io.alchevrier.raft.RaftLogEntry;

final public class InMemoryRaftLog implements RaftLog {

    private CompositeRaftLog raftLog;

    public InMemoryRaftLog() {
        this.raftLog = new CompositeRaftLog(new PassthroughRaftIndexer(() -> 0L), new InMemoryRaftLogger());
    }

    @Override
    public long append(long term, byte[] data) {
        return raftLog.append(term, data);
    }

    @Override
    public RaftLogEntry get(long index) {
        return raftLog.get(index);
    }

    @Override
    public long getLastIndex() {
        return raftLog.getLastIndex();
    }

    @Override
    public long getLastTerm() {
        return raftLog.getLastTerm();
    }

    @Override
    public long getTermAt(long index) {
        return raftLog.getTermAt(index);
    }

    @Override
    public void deleteFrom(long index) {
        raftLog.deleteFrom(index);
    }

    @Override
    public long scanFirst(long from, long to, LogScanFunction fn) {
        return raftLog.scanFirst(from, to, fn);
    }
}
