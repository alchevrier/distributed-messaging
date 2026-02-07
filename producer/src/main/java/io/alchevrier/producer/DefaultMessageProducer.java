package io.alchevrier.producer;

import io.alchevrier.message.ProduceRequest;
import io.alchevrier.message.ProduceResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultMessageProducer implements MessageProducer {

    private final MessageProducerClient client;

    public DefaultMessageProducer(MessageProducerClient client) {
        this.client = client;
    }

    @Override
    public ProduceResponse produce(ProduceRequest produceRequest) {
        return client.produce(produceRequest.topic().name(), produceRequest);
    }
}
