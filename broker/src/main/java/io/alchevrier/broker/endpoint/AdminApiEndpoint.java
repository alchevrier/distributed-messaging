package io.alchevrier.broker.endpoint;

import io.alchevrier.logstorageengine.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminApiEndpoint {

    private final LogManager logManager;

    public AdminApiEndpoint(@Autowired LogManager logManager) {
        this.logManager = logManager;
    }

    @PostMapping("/admin/flush")
    public ResponseEntity<Void> flush() {
        logManager.flush();
        return ResponseEntity.noContent().build();
    }
}
