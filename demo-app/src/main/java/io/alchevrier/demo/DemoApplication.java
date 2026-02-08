package io.alchevrier.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "io.alchevrier.consumer", "io.alchevrier.producer, io.alchevrier.demo" })
public class DemoApplication {
    public static void main(String... args) {
        SpringApplication.run(DemoApplication.class);
    }
}
