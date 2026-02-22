package io.alchevrier.broker.endpoint;

import io.alchevrier.broker.service.TopicsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminApiEndpoint {

    private final TopicsService topicsService;

    public AdminApiEndpoint(TopicsService topicsService) {
        this.topicsService = topicsService;
    }

    @PostMapping("/admin/flush")
    public ResponseEntity<Void> flush() {
        topicsService.flush();
        return ResponseEntity.noContent().build();
    }
}
