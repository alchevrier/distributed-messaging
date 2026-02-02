package io.alchevrier.logstorageengine

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class LogSegmentTest extends Specification {
    @TempDir
    Path tempDir

    LogSegment segment
    String segmentPath

    def setup() {
        segmentPath = tempDir.resolve("test-segment").toString()
        segment = new LogSegmentImpl(segmentPath, 0)
    }

    def cleanup() {
        segment?.close()  // Safe close even if test fails
    }

    def "empty log last offset should be the base offset minus 1"() {
        when: "no write happened"
        then: "should have lastOffset equals to the base offset minus 1"
            segment.lastOffset() == -1
    }

    def "appending to an empty log then should yield size according to entry"() {
        when: "writing data to the log segment then flushing/closing"
            segment.append(0, "Hello World!".getBytes())
            segment.flush()

        then: "should return the correct size"
            segment.size() == 4 + 8 + 12
    }

    def "appending to an empty log then should allow us to read log entry"() {
        when: "writing data to the log segment and flushing/closing"
            segment.append(0, "Hello World!".getBytes())
            segment.flush()
            def result = new String(segment.read(0))

        then: "should read if provided the correct offset"
            result == "Hello World!"
    }

    def "appending to an empty log then should not allow us to read an offset not persisted yet"() {
        when: "writing data to the log segment and flushing/closing"
            segment.append(0, "Hello World!".getBytes())
            segment.flush()
            segment.read(1)

        then: "should read if provided the correct offset"
            thrown RuntimeException
    }

    def "given no log nor index file should create new file when appending"() {
        when: "writing data to the log segment and flushing/closing"
            segment.append(0, "Hello World!".getBytes())
            segment.flush()
        then: "log file should be created and filled with the correct content"
            def bytes = Files.readAllBytes(Paths.get(segmentPath + ".log"))
            def buffer = ByteBuffer.wrap(bytes)

            with (buffer) {
                getInt() == 12
                getLong() == 0

                def data = new byte[12]
                get(data)
                new String(data) == "Hello World!"
                !hasRemaining()
            }

        and: "index file should be created and filled with the correct content"
            def indexBytes = Files.readAllBytes(Paths.get(segmentPath + ".index"))
            def indexBuffer = ByteBuffer.wrap(indexBytes)

            with (indexBuffer) {
                getLong() == 0
                getLong() == 0
                !hasRemaining()
            }

            segment.lastOffset() == 0
    }

    def "appending to a log file should be accessible by a fresh log segment"() {
        when: "writing data to the log segment and flushing/closing"
            segment.append(0, "Hello World!".getBytes())
            segment.flush()
            segment.close()
            segment = new LogSegmentImpl(segmentPath, 1) // starting from the next offset
            def result = new String(segment.read(0))

        then: "log file information should be established as persisted"
            result == "Hello World!"
            segment.size() == 4 + 8 + 12
            segment.lastOffset() == 0
    }

    def "appending to an existing log segment should not overwrite existing data"() {
        when: "writing data to the log segment and flushing/closing then write to an existing new log segment"
            segment.append(0, "Hello World!".getBytes())
            segment.flush()
            segment.close()
            segment = new LogSegmentImpl(segmentPath, 1) // starting from the next offset
            segment.append(1, "This should be accessible".getBytes())
            def result = new String(segment.read(1))

        then: "log file information should be established as persisted"
            result == "This should be accessible"
            segment.size() == (4 + 8 + 12) + 4 + 8 + 25
            segment.lastOffset() == 1
    }

    def "appending multiple times to a log file should be accessible"() {
        when: "writing data to the log segment and flushing/closing"
            segment.append(0, "Hello World!".getBytes())
            segment.append(1, "Second message".getBytes())
            segment.append(2, "Will it work you think?".getBytes())
            segment.flush()

            // reading out of order to check that we can access an offset any time
            def thirdResult = new String(segment.read(2))
            def firstResult = new String(segment.read(0))
            def secondResult = new String(segment.read(1))

        then: "log file information should be established as persisted"
            firstResult == "Hello World!"
            secondResult == "Second message"
            thirdResult == "Will it work you think?"
            segment.size() == (4 + 8 + 12) + (4 + 8 + 14) + (4 + 8 + 23)
            segment.lastOffset() == 2
    }

    def "multiple threads should have no issue reading at the same time"() {
        given: "a log segment with one entry"
            segment.append(0, "Hello World!".getBytes())
            segment.flush()

        when: "1000 virtual threads read the same offset concurrently"
            def executor = Executors.newVirtualThreadPerTaskExecutor()
            def futures = (0..<1000).collect {
                CompletableFuture.supplyAsync({
                    new String(segment.read(0))
                }, executor)
            }
            def results = futures.collect { it.join() }
            executor.close()

        then: "all reads should return the correct data"
            results.size() == 1000
            results.every { it == "Hello World!" }
    }
}
