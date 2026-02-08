package io.alchevrier.producer.configuration;

import io.alchevrier.producer.MessageProducerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ProducerConfiguration {

    @Bean
    public MessageProducerClient producerClient(RestClient producerRestClient) {
        return HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(producerRestClient))
                .build()
                .createClient(MessageProducerClient.class);
    }

    @Bean
    public RestClient producerRestClient(@Value("${client.brokerUrl}") String brokerUrl) {
        return RestClient.builder()
                .baseUrl(brokerUrl)
                .build();
    }
}
