package io.alchevrier.logstorageengine;

import io.alchevrier.message.Topic;

/**
 * Create/retrieve Log for each topic
 */
public interface LogManager {
    long append(Topic topic, byte[] data);
    byte[] read(Topic topic, long offset);
    void close();
    void flush();
}
