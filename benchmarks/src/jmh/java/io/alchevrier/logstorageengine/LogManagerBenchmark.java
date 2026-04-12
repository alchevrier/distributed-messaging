package io.alchevrier.logstorageengine;

import io.alchevrier.message.Topic;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class LogManagerBenchmark {

    private LogManager logManager;
    private byte[] priceOrder;
    private Topic topic;

    @Setup(Level.Trial)
    public void setup() {
        this.logManager = new LogManagerImpl(
                "/tmp/benchmark/logManager/logs", 1000000, 50000, 3
        );
        this.priceOrder = providePriceOrder();
        this.topic = new Topic("test");
    }

    @TearDown
    public void tearDown() {
        this.logManager.close();
        deleteAllFiles();
    }

    @Benchmark
    public void priceAt(Blackhole bh) {
        bh.consume(logManager.append(this.topic, null, this.priceOrder));
    }

    private byte[] providePriceOrder() {
        var buffer = ByteBuffer.allocate(16);
        buffer.putLong(Instant.now().toEpochMilli());
        buffer.putInt(150);
        buffer.putInt(2);
        return buffer.array();
    }

    private void deleteAllFiles() {
        var dir = Paths.get("/tmp/benchmark/logManager/logs");
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
