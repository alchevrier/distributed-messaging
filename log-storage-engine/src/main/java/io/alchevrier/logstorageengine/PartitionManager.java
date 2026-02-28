package io.alchevrier.logstorageengine;

public interface PartitionManager {
    int resolve(String key);
    void incrementCount(int partition, long by);
    long getCount(int partition);
}
