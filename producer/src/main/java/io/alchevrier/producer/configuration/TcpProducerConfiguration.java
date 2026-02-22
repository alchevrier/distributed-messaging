package io.alchevrier.producer.configuration;

import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.producer.MessageProducer;
import io.alchevrier.producer.TcpMessageProducer;
import io.alchevrier.tcpclient.TcpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "client.tcp.enabled", havingValue = "true")
public class TcpProducerConfiguration {
    @Bean
    public MessageProducer tcpMessageProducer(TcpClient tcpClient) {
        return new TcpMessageProducer(tcpClient, new ByteBufferSerializer(), new ByteBufferDeserializer());
    }

    @Bean(destroyMethod = "close")
    public TcpClient tcpClient(@Value("${client.tcp.host}") String host, @Value("${client.tcp.port}") int port) {
        return new TcpClient(host, port);
    }
}
