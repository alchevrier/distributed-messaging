package io.alchevrier.raft.log;

import io.alchevrier.raft.RaftLogEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

final public class FileChannelRaftLogger implements RaftLogger, AutoCloseable {

    private final FileChannel logChannel;
    private final ReentrantLock writeLock;

    public FileChannelRaftLogger(String pathName) {
        var logPath = Path.of(pathName + ".raftlog");

        try {
            Files.createDirectories(logPath.getParent());
            logChannel = FileChannel.open(
                    logPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
            );

            writeLock = new ReentrantLock();

            if (logChannel.size() == 0) {
                append(4 + 8 + 8, 0, 0, new byte[0]);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not do IO operation while initializing the FileChannelRaftLogger for: " + pathName, e);
        }

        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(this::close));
    }

    @Override
    public long append(int length, long index, long term, byte[] data) {
        writeLock.lock();
        try {
            var filePosition = logChannel.size();

            // Format of log entry: [length: 4 bytes][index: 8 bytes][term: 8 bytes][data: variable bytes]
            var buffer = ByteBuffer.allocate(4 + 8 + 8 + data.length);
            buffer.putInt(data.length);
            buffer.putLong(index);
            buffer.putLong(term);
            buffer.put(data);
            buffer.flip();

            logChannel.write(buffer, filePosition);
            return filePosition;
        } catch (IOException e) {
            // think about how to handle this
        } finally {
            writeLock.unlock();
        }
        return -1;
    }

    @Override
    public RaftLogEntry getEntryAt(long filePosition) {
        try {
            var lengthBuf = ByteBuffer.allocate(4);
            logChannel.read(lengthBuf, filePosition);
            lengthBuf.flip();
            int length = lengthBuf.getInt();

            // Read term (skip offset field: 4 + 8 = 12 bytes)
            var termBuf = ByteBuffer.allocate(8);
            logChannel.read(termBuf, filePosition + 12);
            termBuf.flip();
            long term = termBuf.getLong();

            // Read data (skip offset + term field: 4 + 8 + 8 = 20 bytes)
            var dataBuf = ByteBuffer.allocate(length);
            logChannel.read(dataBuf, filePosition + 20);
            dataBuf.flip();

            byte[] data = new byte[dataBuf.remaining()];
            dataBuf.get(data);

            return new RaftLogEntry(term, data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void deleteFrom(long position) {
        if (position == 0) throw new RuntimeException("Cannot delete from sentinel entry");
        try {
            logChannel.truncate(position);
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    @Override
    public void close() {
        try {
            logChannel.force(true);
            logChannel.close();
        } catch (IOException ex) {
            // Will log and silently move on
        }
    }
}
