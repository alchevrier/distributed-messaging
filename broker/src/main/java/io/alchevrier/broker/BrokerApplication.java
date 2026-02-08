package io.alchevrier.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class BrokerApplication {
    static void main(String... args) {
        ConfigurableApplicationContext context = SpringApplication.run(BrokerApplication.class);

        // Ensure graceful shutdown on JVM termination (Ctrl+C, kill, etc.)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down broker gracefully...");
            context.close();
        }));
    }
}
