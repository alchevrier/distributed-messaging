package io.alchevrier.logstorageengine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LogImpl implements Log {

    private final long maxSegmentSize;
    private final TreeMap<Long, LogSegment> segments;
    private final Lock readLock;
    private final Lock writeLock;
    private final String logDirectory;
    private final long flushInterval;
    private long nextOffset;
    private long currentInterval;

    public LogImpl(String logDirectory, long maxSegmentSize, long flushInterval) {
        this.maxSegmentSize = maxSegmentSize;
        this.segments = new TreeMap<>();
        this.logDirectory = logDirectory;
        this.flushInterval = flushInterval;
        this.currentInterval = 0;

        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        this.readLock = rwLock.readLock();
        this.writeLock = rwLock.writeLock();

        try {
            Files.createDirectories(Paths.get(logDirectory));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory: " + logDirectory, e);
        }

        loadExistingSegments();

        if (segments.isEmpty()) {
            createNewSegment(0);
            this.nextOffset = 0;
        } else {
            this.nextOffset = segments.lastEntry().getValue().lastOffset() + 1;
        }
    }

    private void loadExistingSegments() {
        try (var files = Files.list(Paths.get(logDirectory))) {
            files.filter(p -> p.toString().endsWith(".log"))
                    .forEach(logFile -> {
                        String filename = logFile.getFileName().toString();
                        // Parse "00000000000000001234.log" -> 1234
                        try {
                            long baseOffset = Long.parseLong(filename.substring(0, 20));
                            createNewSegment(baseOffset);
                        } catch (NumberFormatException ex) {
                            // will log later on
                            // file should be ignored as not adhering to the code convention
                        }
                    });
        } catch (IOException ex) {
            throw new RuntimeException("Could not load logSegments for log directory: " + logDirectory, ex);
        }
    }

    private void createNewSegment(long atOffset) {
        String segmentPath = logDirectory + "/" + String.format("%020d", atOffset);
        segments.put(atOffset, new LogSegmentImpl(segmentPath, atOffset));
    }

    @Override
    public long append(byte[] data) {
        writeLock.lock();
        try {
            var currentSegment = this.segments.lastEntry().getValue(); // always need the latest offset
            if (currentSegment.size() > maxSegmentSize) {
                currentSegment.flush();
                currentInterval = 0;

                createNewSegment(nextOffset);
                currentSegment = this.segments.lastEntry().getValue();
            }
            currentSegment.append(nextOffset, data);
            var writtenOffset = nextOffset;
            nextOffset = nextOffset + 1;

            currentInterval++;
            if (flushInterval == currentInterval) {
                currentSegment.flush();
                currentInterval = 0;
            }

            return writtenOffset;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public byte[] read(long offset) {
        readLock.lock();
        try {
            var mostAccurateSegment = this.segments.floorEntry(offset);
            if (mostAccurateSegment == null) {
                throw new RuntimeException("Offset " + offset + " is before first segment");
            }
            return mostAccurateSegment.getValue().read(offset);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void flush() {
        writeLock.lock();
        try {
            this.segments.forEach((_, it) -> it.flush());
            currentInterval = 0;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() {
        writeLock.lock();
        try {
            this.segments.forEach((_, it) -> it.close());
        } finally {
            writeLock.unlock();
        }
    }
}
