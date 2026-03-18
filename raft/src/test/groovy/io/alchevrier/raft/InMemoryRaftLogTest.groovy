package io.alchevrier.raft

import spock.lang.Specification

class InMemoryRaftLogTest extends Specification {

    InMemoryRaftLog objectUnderTest

    def setup() {
        objectUnderTest = new InMemoryRaftLog()
    }

    def "by default the last index is 0 when initialized"() {
        when: "getting the last index of initialized log"
            def lastIndex = objectUnderTest.getLastIndex()
        then: "should be zero"
            lastIndex == 0

        when: "getting the 0 index entry"
            def result = objectUnderTest.get(objectUnderTest.getLastIndex())
        then: "then should be the 'no entry' one"
            result.term() == 0
            result.data().length == 0

        when: "deleting the 0 index entry"
            objectUnderTest.deleteFrom(0)
        then: "should throw an error indicating it is not possible"
            thrown IllegalArgumentException

        when: "getting the last term of a log containing only the 'no entry' log entry"
            def lastTerm = objectUnderTest.getLastTerm()
        then: "should return 0 meaning the log is empty"
            lastTerm == 0

        when: "appending three append entries"
            objectUnderTest.append(1, "Hello".getBytes())
            objectUnderTest.append(1, "World".getBytes())
            objectUnderTest.append(2, "123".getBytes())
        then: "should have appended sequentially"
            var firstEntry = objectUnderTest.get(1)
            firstEntry.term() == 1
            new String(firstEntry.data()) == "Hello"

            var secondEntry = objectUnderTest.get(2)
            secondEntry.term() == 1
            new String(secondEntry.data()) == "World"

            var thirdEntry = objectUnderTest.get(3)
            thirdEntry.term() == 2
            new String(thirdEntry.data()) == "123"

        when: "getting the last term"
            def afterAppendingLastTerm = objectUnderTest.getLastTerm()
        then: "should be updated to the latest term"
            afterAppendingLastTerm == 2

        when: "getting the last index"
            def afterAppendingLastIndex = objectUnderTest.getLastIndex()
        then: "should be updated to the latest index"
            afterAppendingLastIndex == 3

        when: "getting the term at indexes"
            def firstTermExample = objectUnderTest.getTermAt(2)
            def secondTermExample = objectUnderTest.getTermAt(3)
        then: "should give me the correct term"
            firstTermExample == 1
            secondTermExample == 2

        when: "delete from the second index onwards and fetching second index"
            objectUnderTest.deleteFrom(2)
            objectUnderTest.get(2)
        then: "should not be available"
            thrown IndexOutOfBoundsException

        when: "fetching third index"
            objectUnderTest.get(3)
        then: "should not be available"
            thrown IndexOutOfBoundsException

        when: "getting the last term"
            def afterDeletionLastTerm = objectUnderTest.getLastTerm()
        then: "should be updated to the latest term"
            afterDeletionLastTerm == 1

        when: "getting the last index"
            def afterDeletionLastIndex = objectUnderTest.getLastIndex()
        then: "should be updated to the latest index"
            afterDeletionLastIndex == 1

        when: "appending after deletion"
            objectUnderTest.append(3, "NewEntry".getBytes())
        then: "should be at index 2 with the new term"
            objectUnderTest.getLastIndex() == 2
            objectUnderTest.getLastTerm() == 3
            new String(objectUnderTest.get(2).data()) == "NewEntry"
    }
}
