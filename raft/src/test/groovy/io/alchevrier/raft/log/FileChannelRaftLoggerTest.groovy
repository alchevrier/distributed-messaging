package io.alchevrier.raft.log

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class FileChannelRaftLoggerTest extends Specification {
    @TempDir
    Path tempDir

    String pathName

    FileChannelRaftLogger raftLogger

    def setup() {
        def path = tempDir.resolve("file-channel-test")
        Files.deleteIfExists(path)
        pathName = path.toString()
    }

    def cleanup() {
        raftLogger?.close()  // Safe close even if test fails
    }

    def "appending multiple times to a file then deleting from - then should correct entry"() {
        given: "a logger"
            raftLogger = new FileChannelRaftLogger(pathName)

            def firstData = "Hello".getBytes()
            def secondData = "World".getBytes()
            def thirdData = "TO BE DELETED".getBytes()
        when: "appending multiple entries"
            def firstEntryPosition= raftLogger.append(4 + 8 + 8 + firstData.length, 1, 1, firstData)
            def secondEntryPosition = raftLogger.append(4 + 8 + 8 + secondData.length, 2, 2, secondData)
            def deletedPosition = raftLogger.append(4 + 8 + 8 + thirdData.length, 3, 3, thirdData)
            raftLogger.append(4 + 8 + 8 + thirdData.length, 4, 3, thirdData)
            raftLogger.append(4 + 8 + 8 + thirdData.length, 5, 3, thirdData)
            raftLogger.append(4 + 8 + 8 + thirdData.length, 6, 3, thirdData)
            raftLogger.deleteFrom(deletedPosition)
        then: "then should provide the correct entries"
            def firstEntry = raftLogger.getEntryAt(firstEntryPosition)
            firstEntry.term() == 1
            new String(firstEntry.data()) == "Hello"

            def secondEntry = raftLogger.getEntryAt(secondEntryPosition)
            secondEntry.term() == 2
            new String(secondEntry.data()) == "World"

            raftLogger.close()

            def recoveredLogger = new FileChannelRaftLogger(pathName)
            def firstEntryRecovered = recoveredLogger.getEntryAt(firstEntryPosition)
            firstEntryRecovered.term() == 1
            new String(firstEntryRecovered.data()) == "Hello"

            def secondEntryRecovered = recoveredLogger.getEntryAt(secondEntryPosition)
            secondEntryRecovered.term() == 2
            new String(secondEntryRecovered.data()) == "World"
    }
}
