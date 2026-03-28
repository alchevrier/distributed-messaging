package io.alchevrier.raft.log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final public class MemoryMappedRaftIndexer implements RaftIndexer, AutoCloseable {

    private final FileChannel indexChannel;
    private final MappedByteBuffer buffer;
    private long writePosition;

    public MemoryMappedRaftIndexer(String indexPathName, long size) {
        var indexPath = Path.of(indexPathName + ".raftindex");

        try {
            Files.createDirectories(indexPath.getParent());

            indexChannel = FileChannel.open(
                    indexPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
            );

            writePosition = indexChannel.size();

            buffer = indexChannel.map(FileChannel.MapMode.READ_WRITE, 0, size);

            if (writePosition == 0) {
                append(0, -1);
                writePosition = 16;
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not do IO operation while initializing the MemoryMappedRaftIndexer for: " + indexPathName, e);
        }

        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(this::close));
    }

    @Override
    public long recover() {
        // Entry count is 16 bytes dividing by 16 is shifting right by 4
        return writePosition >> 4;
    }

    @Override
    public void append(long index, long position) {
        var toBeAppended = ByteBuffer.allocate(8 + 8);
        toBeAppended.putLong(index);
        toBeAppended.putLong(position);
        toBeAppended.flip();

        buffer.put(toBeAppended.array());
        writePosition += 16;
    }

    @Override
    public long getPosition(long index) {
        // Entry count is 16 bytes multiplying by 16 is shifting left by 4
        return buffer.getLong((int) (index << 4) + 8);
    }

    @Override
    public void deleteFrom(long index) {
        buffer.position((int) index << 4);
        writePosition = index << 4;
    }

    @Override
    public void close() {
        try {
            indexChannel.truncate(writePosition);
            indexChannel.force(true);
            indexChannel.close();
        } catch (IOException ex) {
            // Will log and silently move on
        }
    }
}
