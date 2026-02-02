package io.alchevrier.logstorageengine

import spock.lang.Specification
import spock.lang.TempDir

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
            log.read(0)
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
            log.append("Hello World".getBytes())
            log.append("HelloW".getBytes())
        then: "should be able to read all offsets"
            new String(log.read(1)) == "HelloW"
            new String(log.read(0)) == "Hello World"
    }

    def "appending within the segment size then read"() {
        when: "appending no further than the segment size"
            log.append("Hello".getBytes())
            log.append("Wo".getBytes())
        then: "should be able to read all offsets"
            new String(log.read(1)) == "Wo"
            new String(log.read(0)) == "Hello"
    }

    def "appending more than the segment size then create a new log for same directory should fetch existing segments"() {
        when: "appending further than segment size"
            log.append("Hello World".getBytes())
            log.append("HelloW".getBytes())

            log.close()
            log = new LogImpl(logPath, TEST_SEGMENT_SIZE, FLUSH_INTERVAL)
        then: "should be able to read all offsets anyway"
            new String(log.read(1)) == "HelloW"
            new String(log.read(0)) == "Hello World"
    }

    def "appending more than the segment size then when appending again should append at the correct offset"() {
        when: "appending further than segment size"
            log.append("Hello World".getBytes())
            log.append("HelloW".getBytes())

            log.close()
            log = new LogImpl(logPath, TEST_SEGMENT_SIZE, FLUSH_INTERVAL)
            log.append("Should be appended at the right place".getBytes())
        then: "should be able to read all offsets anyway"
            new String(log.read(1)) == "HelloW"
            new String(log.read(2)) == "Should be appended at the right place"
            new String(log.read(0)) == "Hello World"
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
                    new String(log.read(1))
                }, executor)
            }
            def results = futures.collect { it.join() }
            executor.close()
        then: "all reads should return the correct data"
            results.size() == 1000
            results.every { it == "HelloW" }
    }
}
