package io.alchevrier.logstorageengine;

import io.alchevrier.message.Topic;

import java.nio.ByteBuffer;

/**
 * Create/retrieve Log for each topic
 */
public interface LogManager {
    AppendResponse append(Topic topic, String key, byte[] data);
    int read(Topic topic, int partition, long offset, ByteBuffer dest);
    void close();
    void flush();
}
