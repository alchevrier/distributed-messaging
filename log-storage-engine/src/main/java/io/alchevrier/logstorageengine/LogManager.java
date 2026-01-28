package io.alchevrier.logstorageengine;

import io.alchevrier.message.Topic;

/**
 * Create/retrieve Log for each topic
 */
public interface LogManager {
    void append(Topic topic, byte[] data);
    byte[] read(Topic topic, long offset);
}
