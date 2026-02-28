package io.alchevrier.consumer;

import io.alchevrier.message.ConsumeResponse;
import io.alchevrier.message.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Component
public class DefaultMessageConsumer implements MessageConsumer {

    private final MessageConsumerClient consumerClient;

    public DefaultMessageConsumer(@Autowired MessageConsumerClient consumerClient) {
        this.consumerClient = consumerClient;
    }

    @Override
    public ConsumeResponse consume(Topic topic, int partition, long startOffset, int batchSize) {
        var result = consumerClient.consume(topic.name(), startOffset, batchSize, partition);
        if (result.getStatusCode() != HttpStatusCode.valueOf(200)) {
            throw new RuntimeException(String.format("Could not consume at topic %s startOffset %s and batchSize %s", topic, startOffset, batchSize));
        }
        return result.getBody();
    }
}
