package io.alchevrier.consumer.config;

import io.alchevrier.consumer.MessageConsumerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ConsumerConfiguration {

    @Bean
    public MessageConsumerClient consumerClient(RestClient consumerRestClient) {
        return HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(consumerRestClient))
                .build()
                .createClient(MessageConsumerClient.class);
    }

    @Bean
    public RestClient consumerRestClient(@Value("${client.brokerUrl}") String brokerUrl) {
        return RestClient.builder()
                .baseUrl(brokerUrl)
                .build();
    }
}
