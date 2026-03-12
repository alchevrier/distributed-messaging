package io.alchevrier.producer;

import io.alchevrier.message.broker.ProduceRequest;
import io.alchevrier.message.broker.ProduceResponse;
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
