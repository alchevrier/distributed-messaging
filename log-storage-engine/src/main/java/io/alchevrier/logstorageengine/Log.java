package io.alchevrier.logstorageengine;

/**
 * Manage multiple segments for ONE topic
 * - Decides when to roll new segment (default is 100MB)
 * - Find which segment contains offset X
 * - Thread safe
 */
public interface Log {
    long append(byte[] data);
    byte[] read(long offset);
    void flush();
    void close();
}
