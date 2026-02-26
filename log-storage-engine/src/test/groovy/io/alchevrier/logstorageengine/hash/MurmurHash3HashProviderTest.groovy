package io.alchevrier.logstorageengine.hash

import com.google.common.hash.Hashing
import spock.lang.Specification

class MurmurHash3HashProviderTest extends Specification {

    MurmurHash3HashProvider objectUnderTest

    def setup() {
        objectUnderTest = new MurmurHash3HashProvider()
    }

    def "simple input test"() {
        given: "a simple input"
            def input = "hello"
        when: "applying our implementation"
            def result = objectUnderTest.hash(input)
        then: "should match an existing proven one"
            result == Hashing.murmur3_32_fixed(0).hashBytes(input.getBytes()).asInt()
    }
}
