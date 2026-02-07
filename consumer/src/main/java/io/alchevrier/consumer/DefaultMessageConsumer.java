package io.alchevrier.consumer;

import io.alchevrier.message.ConsumeResponse;
import io.alchevrier.message.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

@Component
public class DefaultMessageConsumer implements MessageConsumer {

    private final MessageConsumerClient client;

    public DefaultMessageConsumer(@Autowired MessageConsumerClient client) {
        this.client = client;
    }

    @Override
    public ConsumeResponse consume(Topic topic, long startOffset, long batchSize) {
        var result = client.consume(topic.name(), startOffset, batchSize);
        if (result.getStatusCode() != HttpStatusCode.valueOf(200)) {
            throw new RuntimeException(String.format("Could not consume at topic %s startOffset %s and batchSize %s", topic, startOffset, batchSize));
        }
        return result.getBody();
    }
}
