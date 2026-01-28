package io.alchevrier.logstorageengine;

import io.alchevrier.message.Topic;

public class LogImpl implements Log {

    private long segmentSize = 12500000; // 100MB
    private LogSegment currentSegment;
    private long nextOffset;

    public LogImpl() {

    }

    @Override
    public long append(byte[] data) {
//        if (currentSegment.size() > segmentSize) {
//            // need to switch to a new segment
//        }
//        currentSegment.append(nextOffset, data);
//        return 0;
    }

    @Override
    public byte[] read(long offset) {
//        return new byte[0];
    }
}
