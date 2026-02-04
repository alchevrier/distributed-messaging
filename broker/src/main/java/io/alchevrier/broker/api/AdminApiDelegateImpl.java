package io.alchevrier.broker.api;

import io.alchevrier.logstorageengine.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class AdminApiDelegateImpl implements AdminApiDelegate {

    private final LogManager logManager;

    public AdminApiDelegateImpl(@Autowired LogManager logManager) {
        this.logManager = logManager;
    }

    @Override
    public ResponseEntity<Void> flush() {
        logManager.flush();
        return ResponseEntity.noContent().build();
    }
}
