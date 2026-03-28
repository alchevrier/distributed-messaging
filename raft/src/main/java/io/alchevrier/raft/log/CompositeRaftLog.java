package io.alchevrier.raft.log;

import io.alchevrier.raft.LogScanFunction;
import io.alchevrier.raft.RaftLogEntry;

final public class CompositeRaftLog implements RaftLog {

    private final RaftIndexer raftIndexer;
    private final RaftLogger raftLogger;
    private long currentIndex;

    public CompositeRaftLog(
            RaftIndexer raftIndexer,
            RaftLogger raftLogger
    ) {
        this.raftIndexer = raftIndexer;
        this.raftLogger = raftLogger;

        this.currentIndex = this.raftIndexer.recover();
        if (this.currentIndex == 0) {
            append(0, new byte[0]);
        }
    }

    @Override
    public long append(long term, byte[] data) {
        var position = raftLogger.append(4 + 8 + data.length, currentIndex, term, data);
        if (position == -1) {
            throw new RuntimeException("Could not append data for term (%s)".formatted(term));
        }
        raftIndexer.append(currentIndex, position);
        currentIndex++;
        return currentIndex;
    }

    @Override
    public RaftLogEntry get(long index) {
        return raftLogger.getEntryAt(raftIndexer.getPosition(index));
    }

    @Override
    public long getLastIndex() {
        return currentIndex - 1;
    }

    @Override
    public long getLastTerm() {
        return get(currentIndex - 1).term();
    }

    @Override
    public long getTermAt(long index) {
        return get(index).term();
    }

    @Override
    public void deleteFrom(long index) {
        var positionToDeleteFrom = raftIndexer.getPosition(index);
        raftIndexer.deleteFrom(index);
        raftLogger.deleteFrom(positionToDeleteFrom);
        currentIndex = index;
    }

    @Override
    public long scanFirst(long from, long to, LogScanFunction fn) {
        if (from >= to) {
            for (var i = (int) from; i >= to; i--) {
                if (fn.test(i, get(i))) return i;
            }
        } else {
            for (var i = (int) from; i <= to; i++) {
                if (fn.test(i, get(i))) return i;
            }
        }
        return -1;
    }

    long getCurrentIndex() {
        return this.currentIndex;
    }
}
