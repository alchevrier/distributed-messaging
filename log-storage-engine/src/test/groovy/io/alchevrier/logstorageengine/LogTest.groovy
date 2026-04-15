package io.alchevrier.logstorageengine

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class LogTest extends Specification {
    @TempDir
    Path tempDir

    long TEST_SEGMENT_SIZE = 20
    long FLUSH_INTERVAL = 1

    Log log
    String logPath

    def setup() {
        logPath = tempDir.resolve("test-log").toString()
        log = new LogImpl(logPath, TEST_SEGMENT_SIZE, FLUSH_INTERVAL)
    }

    def cleanup() {
        log?.close()
    }

    def "reading from empty log should throw exception"() {
        when: "no existing logs"
            log.read(0, ByteBuffer.allocate(12 + 1))
        then: "should throw exception as there is no content"
            thrown RuntimeException
    }

    def "appending more than the segment size then should rotate to new segment after appending"() {
        when: "appending further than segment size"
            log.append("Hello World".getBytes())
            log.append("HelloW".getBytes())
        then: "should have log rotated"
            Files.list(Paths.get(logPath)).toArray().length == 4 // 2 * .index + 2 * .log
            // checking file name of rotated log
            Files.list(Paths.get(logPath)).anyMatch { it.toString().endsWith("00000000000000000000.index")  }
            Files.list(Paths.get(logPath)).anyMatch { it.toString().endsWith("00000000000000000000.log")  }
            Files.list(Paths.get(logPath)).anyMatch { it.toString().endsWith("00000000000000000001.index")  }
            Files.list(Paths.get(logPath)).anyMatch { it.toString().endsWith("00000000000000000001.log")  }
    }

    def "appending within the segment size then should not rotate to new segment"() {
        when: "appending no further than the segment size"
            log.append("Hello".getBytes())
            log.append("Wo".getBytes())
        then: "should not have rotated log"
            Files.list(Paths.get(logPath)).toArray().length == 2 // 1 * .index + 1 * .log
            // checking file name of rotated log
            Files.list(Paths.get(logPath)).anyMatch { it.toString().endsWith("00000000000000000000.index")  }
            Files.list(Paths.get(logPath)).anyMatch { it.toString().endsWith("00000000000000000000.log") }
    }

    def "appending more than the segment size  then read"() {
        when: "appending further than segment size"
            def firstSrc = "Hello World".getBytes()
            log.append(firstSrc)
            def secondSrc = "HelloW".getBytes()
            log.append(secondSrc)
        then: "should be able to read all offsets"
            def dest = ByteBuffer.allocate(12 + secondSrc.length)
            log.read(1, dest)
            StandardCharsets.UTF_8.decode(dest).toString() == "HelloW"
            def secondDst = ByteBuffer.allocate(12 + firstSrc.length)
            log.read(0, secondDst)
            StandardCharsets.UTF_8.decode(secondDst).toString() == "Hello World"
    }

    def "appending within the segment size then read"() {
        when: "appending no further than the segment size"
            def firstSrc = "Hello".getBytes()
            log.append(firstSrc)
            def secondSrc = "Wo".getBytes()
            log.append(secondSrc)
        then: "should be able to read all offsets"
            def secondDst = ByteBuffer.allocate(12 + secondSrc.length)
            log.read(1, secondDst)
            StandardCharsets.UTF_8.decode(secondDst).toString() == "Wo"
            def firstDst = ByteBuffer.allocate(12 + firstSrc.length)
            log.read(0, firstDst)
            StandardCharsets.UTF_8.decode(firstDst).toString() == "Hello"
    }

    def "appending more than the segment size then create a new log for same directory should fetch existing segments"() {
        when: "appending further than segment size"
            def firstSrc = "Hello World".getBytes()
            log.append(firstSrc)
            def secondSrc = "HelloW".getBytes()
            log.append(secondSrc)

            log.close()
            log = new LogImpl(logPath, TEST_SEGMENT_SIZE, FLUSH_INTERVAL)
        then: "should be able to read all offsets anyway"
            def secondDst = ByteBuffer.allocate(12 + secondSrc.length)
            log.read(1, secondDst)
            StandardCharsets.UTF_8.decode(secondDst).toString() == "HelloW"
            def firstDst = ByteBuffer.allocate(12 + firstSrc.length)
            log.read(0, firstDst)
            StandardCharsets.UTF_8.decode(firstDst).toString() == "Hello World"
    }

    def "appending more than the segment size then when appending again should append at the correct offset"() {
        when: "appending further than segment size"
            def firstSrc = "Hello World".getBytes()
            log.append(firstSrc)
            def secondSrc = "HelloW".getBytes()
            log.append(secondSrc)

            log.close()
            log = new LogImpl(logPath, TEST_SEGMENT_SIZE, FLUSH_INTERVAL)
            def thirdSrc = "Should be appended at the right place".getBytes()
            log.append(thirdSrc)
        then: "should be able to read all offsets anyway"
            def secondDst = ByteBuffer.allocate(12 + secondSrc.length)
            log.read(1, secondDst)
            StandardCharsets.UTF_8.decode(secondDst).toString() == "HelloW"
            def thirdDst = ByteBuffer.allocate(12 + thirdSrc.length)
            log.read(2, thirdDst)
            StandardCharsets.UTF_8.decode(thirdDst).toString() == "Should be appended at the right place"
            def firstDst = ByteBuffer.allocate(12 + firstSrc.length)
            log.read(0, firstDst)
            StandardCharsets.UTF_8.decode(firstDst).toString() == "Hello World"
    }

    def "concurrent reads across multiple segments should work correctly"() {
        given: "appending further than segment size"
            log.append("Hello World".getBytes())
            log.append("HelloW".getBytes())

            log.close()
            log = new LogImpl(logPath, TEST_SEGMENT_SIZE, FLUSH_INTERVAL)

        when: "1000 virtual threads read the same offset concurrently"
            def executor = Executors.newVirtualThreadPerTaskExecutor()
            def futures = (0..<1000).collect {
                CompletableFuture.supplyAsync({
                    def length = "HelloW".getBytes().length
                    def dst = ByteBuffer.allocate(12 + length)
                    log.read(1, dst)
                    StandardCharsets.UTF_8.decode(dst).toString()
                }, executor)
            }
            def results = futures.collect { it.join() }
            executor.close()
        then: "all reads should return the correct data"
            results.size() == 1000
            results.every { it == "HelloW" }
    }
}
