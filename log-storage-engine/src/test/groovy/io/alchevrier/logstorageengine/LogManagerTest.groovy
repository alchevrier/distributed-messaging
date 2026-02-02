package io.alchevrier.logstorageengine

import io.alchevrier.message.Topic
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class LogManagerTest extends Specification {
    @TempDir
    Path tempDir

    long TEST_SEGMENT_SIZE = 20
    long FLUSH_INTERVAL = 1

    LogManager logManager
    String mainDirectory

    def setup() {
        mainDirectory = tempDir.resolve("main-directory").toString()
        logManager = new LogManagerImpl(mainDirectory, TEST_SEGMENT_SIZE, FLUSH_INTERVAL)
    }

    def cleanup() {
        logManager?.close()
    }

    def "reading from an empty log should throw an exception"() {
        when: "no existing log"
            logManager.read(new Topic("testTopic"), 0)
        then: "should throw exception"
            thrown RuntimeException
    }

    def "appending to more than one log then should create multiple directory for each log"() {
        when: "appending to more than one log"
            logManager.append(new Topic("testTopic"), "Hello World!".getBytes())
            logManager.append(new Topic("secondTopic"), "Hello World!".getBytes())
        then: "should have one directory for each topic"
            Files.list(Paths.get(mainDirectory)).toArray().length == 2
            Files.list(Paths.get(mainDirectory)).anyMatch { it.toString().endsWith("testTopic")  }
            Files.list(Paths.get(mainDirectory)).anyMatch { it.toString().endsWith("secondTopic")  }
    }

    def "appending to more than one log then should be able to read at any of the log"() {
        given: "appending to more than one log"
            logManager.append(new Topic("testTopic"), "Hello World!".getBytes())
            logManager.append(new Topic("secondTopic"), "Is Different".getBytes())
        when: "reading any of the offset of the logs"
            def secondTopicResult = logManager.read(new Topic("secondTopic"), 0)
            def firstTopicResult = logManager.read(new Topic("testTopic"), 0)
        then: "should have the corresponding message as appended"
            new String(secondTopicResult) == "Is Different"
            new String(firstTopicResult) == "Hello World!"
    }

    def "concurrent write on a non-existing log should yield only one log directory created"() {
        when: "1000 virtual threads write the same log concurrently"
            def executor = Executors.newVirtualThreadPerTaskExecutor()
            def futures = (0..<1000).collect {
                CompletableFuture.runAsync({
                    logManager.append(new Topic("testTopic"), "Hello".getBytes())
                }, executor)
            }
            futures.collect { it.join() }
            executor.close()

        then: "only one folder with the topic name should be created"
            Files.list(Paths.get(mainDirectory)).toArray().length == 1
            Files.list(Paths.get(mainDirectory)).anyMatch { it.toString().endsWith("testTopic")  }
            new String(logManager.read(new Topic("testTopic"), 999)) == "Hello"
    }

    def "concurrent reads across multiple segments should work correctly"() {
        given: "appending further than segment size"
            logManager.append(new Topic("testTopic"), "Hello World".getBytes())
            logManager.append(new Topic("testTopic"), "HelloW".getBytes())

        when: "1000 virtual threads read the same offset concurrently"
            def executor = Executors.newVirtualThreadPerTaskExecutor()
            def futures = (0..<1000).collect {
                CompletableFuture.supplyAsync({
                    new String(logManager.read(new Topic("testTopic"), 1))
                }, executor)
            }
            def results = futures.collect { it.join() }
            executor.close()

        then: "all reads should return the correct data"
            results.size() == 1000
            results.every { it == "HelloW" }
    }
}
