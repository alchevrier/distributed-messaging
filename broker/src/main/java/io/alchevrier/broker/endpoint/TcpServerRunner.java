package io.alchevrier.broker.endpoint;

import io.alchevrier.tcpserver.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class TcpServerRunner implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TcpServerRunner.class);

    private final int tcpPort;
    private final TcpServer tcpServer;

    public TcpServerRunner(
            @Value("${binary.server.port}") int tcpPort,
            TcpServer tcpServer
    ) {
        this.tcpPort = tcpPort;
        this.tcpServer = tcpServer;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread.ofVirtual()
                .name("tcp-server")
                .start(() -> {
                    LOGGER.info("Starting TCP server at port: {}", tcpPort);
                    tcpServer.start();
                });
    }
}
