package io.alchevrier.consumer.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.alchevrier.consumer")
public class SmokeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmokeApplication.class, args);
    }
}
