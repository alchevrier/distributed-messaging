package io.alchevrier.broker

import io.alchevrier.broker.api.AdminApi
import io.alchevrier.broker.api.AdminApiDelegate
import io.alchevrier.logstorageengine.LogManager
import org.springframework.http.HttpStatusCode
import spock.lang.Specification

class AdminApiDelegateTest extends Specification {
    AdminApi objectUnderTest
    LogManager mockManager

    def setup() {
        mockManager = Mock(LogManager)
        objectUnderTest = new AdminApiDelegate(mockManager)
    }

    def "flushing should delegate call to the logManager flushing mechanism"() {
        when:
            def result = objectUnderTest.flush()
        then:
            result.statusCode == HttpStatusCode.valueOf(204)

            1 * mockManager.flush()
    }
}
