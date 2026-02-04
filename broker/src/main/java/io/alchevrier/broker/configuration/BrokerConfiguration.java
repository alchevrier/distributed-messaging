package io.alchevrier.broker.configuration;

import io.alchevrier.logstorageengine.LogManager;
import io.alchevrier.logstorageengine.LogManagerImpl;
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
}
