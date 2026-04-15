package io.alchevrier.logstorageengine;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

public class LogSegmentImpl implements LogSegment {

    private final long baseOffset;
    private final FileChannel logChannel;
    private final FileChannel indexChannel;
    private final ReentrantLock writeLock;
    private final Arena writeArena;
    private final MemorySegment indexSegement;
    private final MemorySegment headerSegment;
    private final Arena readArena;
    private final MemorySegment offsetIndexSlab;
    int entryCount;

    public LogSegmentImpl(String segmentPathName, long baseOffset, long maxSegmentSize) {
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

            writeArena = Arena.ofShared();
            indexSegement = writeArena.allocate(16);
            headerSegment = writeArena.allocate(12);

            readArena = Arena.ofShared();
            // 16 bytes is the size of the index record, 12 is the header size which gives us a safe approximation of the memory to allocate off-heap
            offsetIndexSlab = readArena.allocate((maxSegmentSize / 12 + 1) * 16);
            entryCount = 0;
        } catch (IOException ex) {
            throw new RuntimeException("Could not do IO operation while initializing the LogSegment for: " + segmentPathName, ex);
        }

        loadIndex();
        writeLock = new ReentrantLock();
    }

    private void loadIndex() {
        try {
            if (indexChannel.size() == 0) return;

            // Format of index entry: [offset: 8 bytes][filePosition: 8 bytes]
            var buffer = indexSegement.asByteBuffer();

            while (indexChannel.read(buffer) != -1) {
                buffer.flip();

                // Only parse if we read a complete entry (16 bytes)
                if (buffer.remaining() == 16) {
                    long offset = buffer.getLong();
                    long position = buffer.getLong();
                    offsetIndexSlab.set(ValueLayout.JAVA_LONG, entryCount * 16, offset);
                    offsetIndexSlab.set(ValueLayout.JAVA_LONG, entryCount * 16 + 8, position);

                    entryCount++;
                }
                // Partial entry means corrupt/incomplete write - skip it

                buffer.clear();
            }
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
        if (entryCount == 0) return this.baseOffset - 1;
        return offsetIndexSlab.get(ValueLayout.JAVA_LONG, (entryCount - 1) * 16);
    }

    @Override
    public void append(long offset, byte[] data) {
        writeLock.lock();
        try {
            var filePosition = logChannel.size();

            // Format of log entry: [length: 4 bytes][offset: 8 bytes][data: variable bytes]
            headerSegment.set(ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN), 0, data.length);
            headerSegment.set(ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN), 4, offset);

            // Wrap the existing payload - no copy, no on allocation of backing memory
            var payloadBuffer = ByteBuffer.wrap(data);

            logChannel.position(filePosition);
            long written = 0;
            ByteBuffer[] srcs = { headerSegment.asByteBuffer(), payloadBuffer };
            while (written < 12 + data.length) {
                written += logChannel.write(srcs, 0, 2);
            }

            // Format of index entry: [offset: 8 bytes][filePosition: 8 bytes]
            var indexBuffer = indexSegement.asByteBuffer();
            indexBuffer.putLong(offset);
            indexBuffer.putLong(filePosition);
            indexBuffer.flip();

            indexChannel.write(indexBuffer, indexChannel.size());

            offsetIndexSlab.set(ValueLayout.JAVA_LONG, entryCount * 16, offset);
            offsetIndexSlab.set(ValueLayout.JAVA_LONG, entryCount * 16 + 8, filePosition);

            entryCount++;
        } catch (IOException ex) {
            // think about how to handle this
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int read(long offset, ByteBuffer dest) {
        try {
            var filePosition = findFilePositionFromSlab(offset);
            if (filePosition == null) {
                throw new RuntimeException("Asking for an offset not persisted yet");
            }

            // Read data (skip offset field: 4 + 8 = 12 bytes)
            logChannel.read(dest, filePosition);
            dest.flip();
            int length = dest.getInt();
            dest.position(4 + 8);

            return length;
        } catch (IOException ex) {
            // think about how to handle this
            throw new RuntimeException("Not Implemented yet");
        }
    }

    private Long findFilePositionFromSlab(long offset) {
        var lower = 0;
        var upper = entryCount - 1;

        if (offset > offsetAtIndex(upper)) {
            return null;
        }

        if (offset == offsetAtIndex(lower)) return filePositionAtIndex(lower);
        if (offset == offsetAtIndex(upper)) return filePositionAtIndex(upper);

        while (lower < upper) {
            var mid = ((lower + upper) / 2);
            var midOffset = offsetAtIndex(mid);
            if (midOffset == offset) return filePositionAtIndex(mid);
            if (midOffset < offset) lower = mid;
            else upper = mid;
        }

        return null;
    }

    private Long offsetAtIndex(int index) {
        return offsetIndexSlab.get(ValueLayout.JAVA_LONG, index * 16);
    }

    private Long filePositionAtIndex(int index) {
        return offsetIndexSlab.get(ValueLayout.JAVA_LONG, index * 16 + 8);
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
            writeArena.close();
            readArena.close();
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
