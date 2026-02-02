package io.alchevrier.logstorageengine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class LogSegmentImpl implements LogSegment {

    private final long baseOffset;
    private final FileChannel logChannel;
    private final FileChannel indexChannel;
    private final Map<Long, Long> offsetIndex;
    private final ReentrantLock writeLock;

    public LogSegmentImpl(String segmentPathName, long baseOffset) {
        this.baseOffset = baseOffset;

        Path logPath = Path.of(segmentPathName + ".log");
        Path indexPath = Path.of(segmentPathName + ".index");

        // Create parent directory if it doesn't exist
        try {
            Files.createDirectories(logPath.getParent());

            logChannel = FileChannel.open(
                    logPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
            );
            indexChannel = FileChannel.open(
                    indexPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new RuntimeException("Could not do IO operation while initializing the LogSegment for: " + segmentPathName, ex);
        }

        offsetIndex = loadIndex();
        writeLock = new ReentrantLock();
    }

    private Map<Long, Long> loadIndex() {
        var loadedOffsetIndex = new HashMap<Long, Long>();
        try {
            if (indexChannel.size() == 0) return loadedOffsetIndex;

            // Format of index entry: [offset: 8 bytes][filePosition: 8 bytes]
            var buffer = ByteBuffer.allocate(8 + 8);

            while (indexChannel.read(buffer) > 0) {
                buffer.flip();

                long offset = buffer.getLong();
                long position = buffer.getLong();
                loadedOffsetIndex.put(offset, position);

                buffer.clear();
            }

            return loadedOffsetIndex;
        } catch (IOException e) {
            throw new RuntimeException("Not Implemented yet");
        }
    }

    @Override
    public long baseOffset() {
        return this.baseOffset;
    }

    @Override
    public long lastOffset() {
        if (this.offsetIndex.isEmpty()) return this.baseOffset - 1;
        return Collections.max(this.offsetIndex.keySet());
    }

    @Override
    public void append(long offset, byte[] data) {
        writeLock.lock();
        try {
            var filePosition = logChannel.size();

            // Format of log entry: [length: 4 bytes][offset: 8 bytes][data: variable bytes]
            var buffer = ByteBuffer.allocate(4 + 8 + data.length);
            buffer.putInt(data.length);
            buffer.putLong(offset);
            buffer.put(data);
            buffer.flip();

            logChannel.write(buffer, filePosition);

            // Format of index entry: [offset: 8 bytes][filePosition: 8 bytes]
            var indexBuffer = ByteBuffer.allocate(8 + 8);
            indexBuffer.putLong(offset);
            indexBuffer.putLong(filePosition);
            indexBuffer.flip();

            indexChannel.write(indexBuffer, filePosition);
            offsetIndex.put(offset, filePosition);
        } catch (IOException ex) {
            // think about how to handle this
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public byte[] read(long offset) {
        try {
            var filePosition = offsetIndex.get(offset);
            if (filePosition == null) {
                throw new RuntimeException("Asking for an offset not persisted yet");
            }

            // Read length (4 bytes)
            ByteBuffer lengthBuf = ByteBuffer.allocate(4);
            logChannel.read(lengthBuf, filePosition);
            lengthBuf.flip();
            int length = lengthBuf.getInt();

            // Read data (skip offset field: 4 + 8 = 12 bytes)
            ByteBuffer dataBuf = ByteBuffer.allocate(length);
            logChannel.read(dataBuf, filePosition + 12);
            dataBuf.flip();

            byte[] result = new byte[dataBuf.remaining()];
            dataBuf.get(result);
            return result;
        } catch (IOException ex) {
            // think about how to handle this
            throw new RuntimeException("Not Implemented yet");
        }
    }

    @Override
    public long size() {
        try {
            return logChannel.size();
        } catch (IOException e) {
            // think about how to handle this
            throw new RuntimeException("Not Implemented yet");
        }
    }

    @Override
    public void close() {
        try {
            logChannel.close();
            indexChannel.close();
        } catch (IOException ex) {
            // Will log and silently move on
        }
    }

    @Override
    public void flush() {
        try {
            logChannel.force(true);
            indexChannel.force(true);
        } catch (IOException ex) {
            // Will log and silently move on
        }
    }
}
