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
    private final int partitionNumber;
    private Map<String, Log> logsPerPartition;
    private Map<Topic, PartitionManager> partitionManagerPerTopics;

    public LogManagerImpl(
            String logDirectory,
            long maxSegmentSize,
            long flushInterval,
            int partitionNumber
    ) {
        this.logDirectory = logDirectory;
        this.maxSegmentSize = maxSegmentSize;
        this.flushInterval = flushInterval;
        this.partitionNumber = partitionNumber;

        try {
            Files.createDirectories(Paths.get(logDirectory));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory: " + logDirectory, e);
        }

        loadExistingTopics();
    }

    private void loadExistingTopics() {
        logsPerPartition = new ConcurrentHashMap<>();
        partitionManagerPerTopics = new ConcurrentHashMap<>();

        try (var files = Files.list(Paths.get(logDirectory))) {
            files.filter(Files::isDirectory).forEach(it -> {
                var partitionName = it.getFileName().toString();
                var log = new LogImpl(logDirectory + "/" + partitionName, maxSegmentSize, flushInterval);
                logsPerPartition.put(partitionName, log);

                var topicName = partitionName.substring(0, partitionName.lastIndexOf("-"));
                var partitionManager = partitionManagerPerTopics.computeIfAbsent(
                        new Topic(topicName),
                        _ -> new DefaultPartitionManager(partitionNumber)
                );

                var partitionNo = Integer.parseInt(partitionName.substring(partitionName.lastIndexOf("-") + 1));
                partitionManager.incrementCount(partitionNo, log.messageCount());
            });
        } catch (IOException ex) {
            throw new RuntimeException("Could not load logs for main directory: " + logDirectory, ex);
        }
    }

    @Override
    public AppendResponse append(Topic topic, String key, byte[] data) {
        var partitionManager = partitionManagerPerTopics.computeIfAbsent(
                topic,
                _ -> new DefaultPartitionManager(partitionNumber)
        );

        var partitionNumber = partitionManager.resolve(key);
        var topicRetrieved = logsPerPartition.computeIfAbsent(
                topic.name() + "-" + partitionNumber,
                it -> new LogImpl(logDirectory + "/" + it, maxSegmentSize, flushInterval)
        );

        var offset = topicRetrieved.append(data);
        partitionManager.incrementCount(partitionNumber, 1);

        return new AppendResponse(partitionNumber, offset);
    }

    @Override
    public byte[] read(Topic topic, int partition, long offset) {
        var partitionManager = partitionManagerPerTopics.get(topic);
        if (partitionManager == null) {
            // figure out what to do here CREATE or throw Exception?
            throw new RuntimeException("Not implemented yet");
        }
        var topicRetrieved = logsPerPartition.get(topic.name() + "-" + partition);
        if (topicRetrieved == null) {
            // figure out what to do here CREATE or throw Exception?
            throw new RuntimeException("Not implemented yet");
        }
        return topicRetrieved.read(offset);
    }

    @Override
    public void close() {
        logsPerPartition.values().forEach(Log::close);
    }

    @Override
    public void flush() {
        logsPerPartition.values().forEach(Log::flush);
    }
}
