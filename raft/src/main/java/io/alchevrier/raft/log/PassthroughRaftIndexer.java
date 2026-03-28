package io.alchevrier.raft.log;

import java.util.function.Supplier;

final public class PassthroughRaftIndexer implements RaftIndexer {

    final private Supplier<Long> recoverHandler;

    public PassthroughRaftIndexer(Supplier<Long> recoverHandler) {
        this.recoverHandler = recoverHandler;
    }

    @Override
    public long recover() {
        return recoverHandler.get();
    }

    @Override
    public void append(long index, long position) {
        // No-op your logger is using index as position
    }

    @Override
    public long getPosition(long index) {
        return index;
    }

    @Override
    public void deleteFrom(long index) {
        // No-op your logger should be doing the deletion by index values
    }
}
