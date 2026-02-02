package io.alchevrier.logstorageengine;

import io.alchevrier.message.Topic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogManagerImpl implements LogManager {
    private final String logDirectory;
    private final long maxSegmentSize;
    private final long flushInterval;
    private Map<Topic, Log> logsPerTopic;

    public LogManagerImpl(String logDirectory, long maxSegmentSize, long flushInterval) {
        this.logDirectory = logDirectory;
        this.maxSegmentSize = maxSegmentSize;
        this.flushInterval = flushInterval;

        try {
            Files.createDirectories(Paths.get(logDirectory));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory: " + logDirectory, e);
        }

        loadExistingTopics();
    }

    private void loadExistingTopics() {
        logsPerTopic = new ConcurrentHashMap<>();

        try (var files = Files.list(Paths.get(logDirectory))) {
            files.filter(Files::isDirectory).forEach(it -> {
                var topicName = it.getFileName().toString();
                logsPerTopic.put(new Topic(topicName), new LogImpl(logDirectory + "/" + topicName, maxSegmentSize, flushInterval));
            });
        } catch (IOException ex) {
            throw new RuntimeException("Could not load logs for main directory: " + logDirectory, ex);
        }
    }

    @Override
    public long append(Topic topic, byte[] data) {
        var topicRetrieved = logsPerTopic.computeIfAbsent(
                topic,
                it -> new LogImpl(logDirectory + "/" + it.name(), maxSegmentSize, flushInterval)
        );
        return topicRetrieved.append(data);
    }

    @Override
    public byte[] read(Topic topic, long offset) {
        var topicRetrieved = logsPerTopic.get(topic);
        if (topicRetrieved == null) {
            // figure out what to do here CREATE or throw Exception?
            throw new RuntimeException("Not implemented yet");
        }
        return topicRetrieved.read(offset);
    }

    @Override
    public void close() {
        logsPerTopic.values().forEach(Log::close);
    }

    @Override
    public void flush() {
        logsPerTopic.values().forEach(Log::flush);
    }
}
