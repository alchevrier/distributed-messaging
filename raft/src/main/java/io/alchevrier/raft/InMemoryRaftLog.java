package io.alchevrier.raft;

import java.util.ArrayList;
import java.util.List;

public class InMemoryRaftLog implements RaftLog {

    private List<RaftLogEntry> entries;

    public InMemoryRaftLog() {
        this.entries = new ArrayList<>();
        // placeholder to make it 1-based
        entries.add(new RaftLogEntry(0, new byte[0]));
    }

    @Override
    public long append(long term, byte[] data) {
        entries.add(new RaftLogEntry(term, data));
        return entries.size();
    }

    @Override
    public RaftLogEntry get(long index) {
        return entries.get((int) index);
    }

    @Override
    public long getLastIndex() {
        return entries.size() - 1;
    }

    @Override
    public long getLastTerm() {
        return entries.getLast().term();
    }

    @Override
    public long getTermAt(long index) {
        return entries.get((int) index).term();
    }

    @Override
    public void deleteFrom(long index) {
        if (index < 1) throw new IllegalArgumentException("Cannot delete the 'no entry' log entry");
        entries.subList((int) index, entries.size()).clear();
    }

    @Override
    public long scanFirst(long from, long to, LogScanFunction fn) {
        if (from >= to) {
            for (var i = (int) from; i >= to; i--) {
                if (fn.test(i, entries.get(i))) return i;
            }
        } else {
            for (var i = (int) from; i <= to; i++) {
                if (fn.test(i, entries.get(i))) return i;
            }
        }
        return -1;
    }
}
