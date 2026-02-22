package io.alchevrier.consumer.configuration;

import io.alchevrier.consumer.MessageConsumer;
import io.alchevrier.consumer.TcpMessageConsumer;
import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.tcpclient.TcpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "client.tcp.enabled", havingValue = "true")
public class TcpConfiguration {

    @Bean
    public MessageConsumer tcpMessageConsumer(TcpClient tcpClient) {
        return new TcpMessageConsumer(tcpClient, new ByteBufferSerializer(), new ByteBufferDeserializer());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(TcpClient.class)
    public TcpClient tcpClient(@Value("${client.tcp.host}") String host, @Value("${client.tcp.port}") int port) {
        return new TcpClient(host, port);
    }
}
