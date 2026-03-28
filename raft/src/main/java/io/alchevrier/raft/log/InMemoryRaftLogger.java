package io.alchevrier.raft.log;

import io.alchevrier.raft.RaftLogEntry;

import java.util.ArrayList;
import java.util.List;

final public class InMemoryRaftLogger implements RaftLogger {

    private List<RaftLogEntry> entries;

    public InMemoryRaftLogger() {
        this.entries = new ArrayList<>();
    }

    @Override
    public long append(int length, long index, long term, byte[] data) {
        entries.add(new RaftLogEntry(term, data));
        return entries.size() - 1;
    }

    @Override
    public RaftLogEntry getEntryAt(long index) {
        // In-Memory considers position == index
        return entries.get((int) index);
    }

    @Override
    public void deleteFrom(long index) {
        if (index < 1) throw new IllegalArgumentException("Cannot delete the 'no entry' log entry");
        entries.subList((int) index, entries.size()).clear();
    }
}
