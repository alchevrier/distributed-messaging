package io.alchevrier.raft.log

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class MemoryMappedRaftIndexerTest extends Specification {
    @TempDir
    Path tempDir

    String pathName

    MemoryMappedRaftIndexer indexer

    def setup() {
        def path = tempDir.resolve("memory-mapped-test")
        Files.deleteIfExists(path)
        pathName = path.toString()
    }

    def cleanup() {
        indexer?.close()  // Safe close even if test fails
    }

    def "appending multiple times to the file - should give correct position"() {
        given: "a new indexer"
            indexer = new MemoryMappedRaftIndexer(pathName, 1000L)
        when: "when appending multiple times to it"
            indexer.append(1, 10)
            indexer.append(2, 18)
            indexer.append(3, 20)
        then: "then should provide the correct position"
            indexer.getPosition(1) == 10
            indexer.getPosition(3) == 20
            indexer.getPosition(2) == 18

            indexer.close()
            def recoveredIndexer = new MemoryMappedRaftIndexer(pathName, 1000L)
            recoveredIndexer.recover() == 3
            recoveredIndexer.getPosition(1) == 10
            recoveredIndexer.getPosition(3) == 20
            recoveredIndexer.getPosition(2) == 18

    }
}
