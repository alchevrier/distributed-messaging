package io.alchevrier.producer.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.alchevrier.producer")
public class SmokeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmokeApplication.class, args);
    }
}
