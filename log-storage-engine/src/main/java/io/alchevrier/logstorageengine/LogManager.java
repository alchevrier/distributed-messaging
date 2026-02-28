package io.alchevrier.logstorageengine;

import io.alchevrier.message.Topic;

/**
 * Create/retrieve Log for each topic
 */
public interface LogManager {
    AppendResponse append(Topic topic, String key, byte[] data);
    byte[] read(Topic topic, int partition, long offset);
    void close();
    void flush();
}
