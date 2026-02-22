package io.alchevrier.broker.endpoint

import io.alchevrier.broker.service.TopicsService
import org.springframework.http.HttpStatusCode
import spock.lang.Specification

class AdminApiEndpointTest extends Specification {
    AdminApiEndpoint objectUnderTest
    TopicsService topicsService

    def setup() {
        topicsService = Mock(TopicsService)
        objectUnderTest = new AdminApiEndpoint(topicsService)
    }

    def "flushing should delegate call to the logManager flushing mechanism"() {
        when:
            def result = objectUnderTest.flush()
        then:
            result.statusCode == HttpStatusCode.valueOf(204)
            1 * topicsService.flush()
    }
}
