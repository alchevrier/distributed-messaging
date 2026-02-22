package io.alchevrier.broker.endpoint

import io.alchevrier.consumer.MessageConsumer
import io.alchevrier.consumer.configuration.TcpConfiguration
import io.alchevrier.message.ConsumeResponse
import io.alchevrier.message.ProduceRequest
import io.alchevrier.message.Topic
import io.alchevrier.producer.MessageProducer
import io.alchevrier.producer.configuration.TcpProducerConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification
import spock.lang.Stepwise
import tools.jackson.databind.ObjectMapper

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@Stepwise
@ActiveProfiles("test")
@Import([TcpProducerConfiguration.class, TcpConfiguration.class])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrokerItTest extends Specification {
    static String logDirectory = './tmp/broker/logs'

    @Autowired
    WebApplicationContext context

    @Autowired
    MessageConsumer tcpMessageConsumer

    @Autowired
    MessageProducer tcpMessageProducer

    MockMvc mockMvc
    ObjectMapper objectMapper

    def setupSpec() {
        cleanupLogDirectory()
    }

    def cleanupLogDirectory() {
        try {
            Files.walk(Paths.get(logDirectory))
                    .sorted(Comparator.reverseOrder())  // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                            // handle
                        }
                    })
        } catch (Exception e) {
            // do nothing as folder does not exists meaning it is already clean
        }
    }

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
        objectMapper = new ObjectMapper()
    }

    def "test produce endpoint"() {
        when:
            def data = Base64.getEncoder().encodeToString("Hello".getBytes())
            def response = mockMvc.perform(
                    post("/topics/test-topic/produce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"data":"${data}"}""")
            ).andReturn().response
        then:
            response.status == 200
    }

    def "test consume endpoint"() {
        when:
            def response = mockMvc.perform(
                    get("/topics/test-topic/consume")
                            .queryParam("offset", "0")
                            .queryParam("batchSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
            ).andReturn().response
        then:
            response.status == 200
            def result = objectMapper.readValue(response.contentAsString, ConsumeResponse)
            result.messages().size() == 1
            result.messages()[0].offset() == 0
            new String(result.messages()[0].data()) == "Hello"
    }

    def "consume tcp via messaging"() {
        when: "sending a consume request message to the tcp server"
            def response = tcpMessageConsumer.consume(new Topic("test-topic"), 0, 100)
        then: "should receive consume response message"
            !response.error
            response.messages().size() == 1
            response.messages()[0].offset() == 0
            new String(response.messages()[0].data()) == "Hello"
    }

    def "test produce a lot more messages concurrently endpoint"() {
        when:
            def executor = Executors.newVirtualThreadPerTaskExecutor()
            def futures = (0..<99).collect {
                CompletableFuture.supplyAsync({
                    return tcpMessageProducer.produce(new ProduceRequest(new Topic("test-topic"), "Hello".getBytes()))
                }, executor)
            }
            def results = futures.collect { it.join() }
            executor.close()

        then:
            results.stream().allMatch {!it.error  }
    }

    def "test consume a lot more messages"() {
        when:
            def response = mockMvc.perform(
                    get("/topics/test-topic/consume")
                            .queryParam("offset", "0")
                            .queryParam("batchSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
            ).andReturn().response
        then:
            response.status == 200
            def result = objectMapper.readValue(response.contentAsString, ConsumeResponse)
            result.messages().size() == 100
            for (i in 0..99) {
                result.messages()[i].offset() == i
                new String(result.messages()[i].data()) == "Hello"
            }
    }
}
