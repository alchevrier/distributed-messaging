package io.alchevrier.logstorageengine;

/**
 * Manage ONE .log file and ONE .index file
 * - Stores message sequentially
 * - Build/use index for lookups
 */
public interface LogSegment {
    long baseOffset();
    void append(long offset, byte[] data);
    byte[] read(long offset);
    long size();
    void close();
    void flush();
}
