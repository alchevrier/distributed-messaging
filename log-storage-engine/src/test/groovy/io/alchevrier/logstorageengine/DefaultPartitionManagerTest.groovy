package io.alchevrier.logstorageengine

import spock.lang.Specification

class DefaultPartitionManagerTest extends Specification {

    DefaultPartitionManager objectUnderTest

    def setup() {
        objectUnderTest = new DefaultPartitionManager(3)
    }

    def "resolve - given different keys should resolve to corresponding partition"() {
        given: "three different keys"
            def firstKey = "Key"
            def secondKey = "Ano"
            def thirdKey = "b"
        when: "resolving"
            def firstResult = objectUnderTest.resolve(firstKey)
            def secondResult = objectUnderTest.resolve(secondKey)
            def thirdResult = objectUnderTest.resolve(thirdKey)
        then: "should assign to partitions"
            firstResult == 1
            secondResult == 2
            thirdResult == 0
    }

    def "resolve - given a null key should resolve to least used partition"() {
        given: "a load"
            objectUnderTest.incrementCount(1, 12)
            objectUnderTest.incrementCount(0, 21)
            objectUnderTest.incrementCount(2, 2)
        when: "resolving with null key"
            def result = objectUnderTest.resolve(null)
        then: "should resolve to least used partition"
            result == 2
    }

    def "resolve - given a null key should resolve to first partition if all partition are equals"() {
        given: "an even load"
            objectUnderTest.incrementCount(1, 12)
            objectUnderTest.incrementCount(0, 12)
            objectUnderTest.incrementCount(2, 12)
        when: "resolving with null key"
            def result = objectUnderTest.resolve(null)
        then: "should resolve to least used partition"
            result == 0
    }
}
