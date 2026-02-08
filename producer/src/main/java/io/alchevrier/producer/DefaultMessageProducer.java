package io.alchevrier.producer;

import io.alchevrier.message.ProduceRequest;
import io.alchevrier.message.ProduceResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultMessageProducer implements MessageProducer {

    private final MessageProducerClient producerClient;

    public DefaultMessageProducer(MessageProducerClient producerClient) {
        this.producerClient = producerClient;
    }

    @Override
    public ProduceResponse produce(ProduceRequest produceRequest) {
        return producerClient.produce(produceRequest.topic().name(), produceRequest);
    }
}
