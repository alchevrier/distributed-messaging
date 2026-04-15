package io.alchevrier.logstorageengine;

import java.nio.ByteBuffer;

/**
 * Manage multiple segments for ONE topic
 * - Decides when to roll new segment (default is 100MB)
 * - Find which segment contains offset X
 * - Thread safe
 */
public interface Log {
    long append(byte[] data);
    int read(long offset, ByteBuffer dest);
    void flush();
    void close();
    long messageCount();
}
