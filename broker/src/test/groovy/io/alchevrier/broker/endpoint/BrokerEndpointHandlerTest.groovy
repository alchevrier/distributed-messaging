package io.alchevrier.broker.endpoint

import io.alchevrier.broker.service.TopicsService
import io.alchevrier.message.ConsumeRequest
import io.alchevrier.message.ConsumeResponse
import io.alchevrier.message.FlushResponse
import io.alchevrier.message.ProduceRequest
import io.alchevrier.message.ProduceResponse
import io.alchevrier.message.Topic
import io.alchevrier.message.serializer.ByteBufferDeserializer
import io.alchevrier.message.serializer.ByteBufferSerializer
import spock.lang.Specification

import java.nio.ByteBuffer

import static io.alchevrier.message.serializer.MessageType.*;

class BrokerEndpointHandlerTest extends Specification {

    ByteBufferSerializer serializer
    ByteBufferDeserializer deserializer
    TopicsService topicsService
    BrokerEndpointHandler objectUnderTest

    def setup() {
        serializer = Mock(ByteBufferSerializer)
        deserializer = Mock(ByteBufferDeserializer)
        topicsService = Mock(TopicsService)
        objectUnderTest = new BrokerEndpointHandler(serializer, deserializer, topicsService)
    }

    def "receiving a consume request"() {
        given: "a consume request"
            def consumeRequest = ByteBuffer.allocate(1);
            consumeRequest.put(CONSUME_REQUEST)
            consumeRequest.flip()

            def mockRequest = new ConsumeRequest(new Topic("test"), 0, 0, 100)
            def mockResponse = Mock(ConsumeResponse)
            def expectedResponse = new byte[0]
        when: "handling the consume request"
            def result = objectUnderTest.handle(consumeRequest.array())
        then: "should have been handled correctly"
            1 * deserializer.deserializeConsumeRequest(consumeRequest.array()) >> mockRequest
            1 * topicsService.consume("test", 0, 0, 100) >> mockResponse
            1 * serializer.serialize(mockResponse) >> expectedResponse

            result == expectedResponse
    }

    def "receiving a produce request"() {
        given: "a produce request"
            def produceRequest = ByteBuffer.allocate(1);
            produceRequest.put(PRODUCE_REQUEST)
            produceRequest.flip()

            def producedData = "To Be Produced".getBytes()
            def mockRequest = new ProduceRequest(new Topic("test"), null, producedData)
            def mockResponse = Mock(ProduceResponse)
            def expectedResponse = new byte[0]
        when: "handling the produce request"
            def result = objectUnderTest.handle(produceRequest.array())
        then: "should have been handled correctly"
            1 * deserializer.deserializeProduceRequest(produceRequest.array()) >> mockRequest
            1 * topicsService.produce("test", null, producedData) >> mockResponse
            1 * serializer.serialize(mockResponse) >> expectedResponse

            result == expectedResponse
    }

    def "receiving a flush request"() {
        given: "a flush request"
            def flushRequest = ByteBuffer.allocate(1);
            flushRequest.put(FLUSH_REQUEST)
            flushRequest.flip()

            def expectedResponse = new byte[0]
        when: "handling the flush request"
            def result = objectUnderTest.handle(flushRequest.array())
        then: "should have been handled correctly"
            1 * topicsService.flush()
            1 * serializer.serialize(new FlushResponse(null)) >> expectedResponse

            result == expectedResponse
    }

    def "receiving a flush request yielded an exception"() {
        given: "a flush request"
            def flushRequest = ByteBuffer.allocate(1);
            flushRequest.put(FLUSH_REQUEST)
            flushRequest.flip()

            def expectedResponse = new byte[0]
        when: "handling the flush request"
            def result = objectUnderTest.handle(flushRequest.array())
        then: "should have been handled the failure as expected"
            1 * topicsService.flush() >> { throw new RuntimeException("Controlled Exception") }
            1 * serializer.serialize(new FlushResponse("Controlled Exception")) >> expectedResponse

            result == expectedResponse
    }

    def "receiving a non recognized request then should throw an exception"() {
        given: "an unknown request"
            def unknownRequest = ByteBuffer.allocate(1);
            unknownRequest.put((byte) 0x99)
            unknownRequest.flip()
        when: "handling the unknown request"
            objectUnderTest.handle(unknownRequest.array())
        then: "should have been handled the failure as expected"
            def thrownE = thrown(IllegalArgumentException)
            thrownE.message == "Did not recognize messageType for type: -103"
    }
}
