package io.alchevrier.demo;

import io.alchevrier.consumer.MessageConsumer;
import io.alchevrier.message.ConsumeResponse;
import io.alchevrier.message.ProduceRequest;
import io.alchevrier.message.ProduceResponse;
import io.alchevrier.message.Topic;
import io.alchevrier.producer.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class DemoController {

    @Autowired
    private MessageConsumer messageConsumer;

    @Autowired
    private MessageProducer messageProducer;


    @GetMapping("/consume")
    public ConsumeResponse consume() {
        return messageConsumer.consume(new Topic("Hello"), 0, 0, 100);
    }

    @PostMapping("/produce")
    public ProduceResponse produce() {
        return messageProducer.produce(new ProduceRequest(new Topic("Hello"), null, ("Hi " + LocalDateTime.now()).getBytes()));
    }
}
