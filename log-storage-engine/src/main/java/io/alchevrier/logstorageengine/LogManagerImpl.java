package io.alchevrier.logstorageengine;

import io.alchevrier.message.Topic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogManagerImpl implements LogManager {
    private Map<Topic, Log> logsPerTopic;

    public LogManagerImpl() {
        logsPerTopic = new ConcurrentHashMap<>();
    }

    @Override
    public void append(Topic topic, byte[] data) {
//        var topicRetrieved = logsPerTopic.get(topic);
//        if (topicRetrieved == null) {
//            // figure out what to do here CREATE or throw Exception?
//            //logsPerTopic.put(topic, );
//        }
//        topicRetrieved.append(data);
    }

    @Override
    public byte[] read(Topic topic, long offset) {
//        var topicRetrieved = logsPerTopic.get(topic);
//        if (topicRetrieved == null) {
//            // figure out what to do here CREATE or throw Exception?
//            throw new RuntimeException("Not implemented yet");
//        }
//        return topicRetrieved.read(offset);
    }
}
