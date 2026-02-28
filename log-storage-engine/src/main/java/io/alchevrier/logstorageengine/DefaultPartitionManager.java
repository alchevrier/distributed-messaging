package io.alchevrier.logstorageengine;

import io.alchevrier.logstorageengine.hash.HashProvider;
import io.alchevrier.logstorageengine.hash.MurmurHash3HashProvider;

import java.util.concurrent.atomic.AtomicLongArray;

public class DefaultPartitionManager implements PartitionManager {

    private final AtomicLongArray currentCounts;
    private final HashProvider hashProvider;

    public DefaultPartitionManager(int numberOfPartition) {
        currentCounts = new AtomicLongArray(numberOfPartition);
        hashProvider = new MurmurHash3HashProvider();
    }

    @Override
    public int resolve(String key) {
        if (key == null) {
            var leastUsedPartition = 0;
            for (var i = 1; i < currentCounts.length(); i++) {
                if (currentCounts.get(leastUsedPartition) > currentCounts.get(i)) {
                    leastUsedPartition = i;
                }
            }
            return leastUsedPartition;
        }
        return (hashProvider.hash(key) & 0x7fffffff) % currentCounts.length();
    }

    @Override
    public void incrementCount(int partition, long by) {
        currentCounts.addAndGet(partition, by);
    }

    @Override
    public long getCount(int partition) {
        return currentCounts.get(partition);
    }
}
