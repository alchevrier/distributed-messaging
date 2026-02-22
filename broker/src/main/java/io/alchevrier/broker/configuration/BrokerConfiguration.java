package io.alchevrier.broker.configuration;

import io.alchevrier.broker.endpoint.BrokerEndpointHandler;
import io.alchevrier.broker.service.TopicsService;
import io.alchevrier.logstorageengine.LogManager;
import io.alchevrier.logstorageengine.LogManagerImpl;
import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.tcpserver.TcpServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfiguration {

    @Bean(destroyMethod = "close")
    public LogManager logManager(
            @Value("${broker.log-directory}") String logDirectory,
            @Value("${broker.max-segment-size}") long maxSegmentSize,
            @Value("${broker.flush-interval}") long flushInterval
    ) {
        return new LogManagerImpl(logDirectory, maxSegmentSize, flushInterval);
    }

    @Bean(destroyMethod = "close")
    public TcpServer tcpServer(
            @Value("${binary.server.port}") int tcpPort,
            BrokerEndpointHandler brokerEndpointHandler
    ) {
        return new TcpServer(tcpPort, brokerEndpointHandler);
    }

    @Bean
    public BrokerEndpointHandler brokerEndpointHandler(TopicsService topicsService) {
        return new BrokerEndpointHandler(
                new ByteBufferSerializer(),
                new ByteBufferDeserializer(),
                topicsService
        );
    }
}
