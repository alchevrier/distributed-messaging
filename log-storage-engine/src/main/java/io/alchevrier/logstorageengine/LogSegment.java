package io.alchevrier.logstorageengine;

import java.nio.ByteBuffer;

/**
 * Manage ONE .log file and ONE .index file
 * - Stores message sequentially
 * - Build/use index for lookups
 */
public interface LogSegment {
    long baseOffset();
    long lastOffset();
    void append(long offset, byte[] data);
    // ByteBuffer should be sized to the whole size of the record (length: 4 bytes, offset: 8 bytes, data: length bytes)
    int read(long offset, ByteBuffer dest);
    long size();
    void close();
    void flush();
}
