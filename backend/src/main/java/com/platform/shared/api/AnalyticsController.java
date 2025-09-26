package com.platform.shared.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.platform.shared.events.AnalyticsEvent;

/**
 * Analytics endpoint for receiving frontend performance metrics.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @PostMapping
    public ResponseEntity<Void> receiveMetrics(@RequestBody AnalyticsRequest request) {
        try {
            logger.info("Received {} analytics events", request.events().size());

            // Process events (store in database, forward to analytics service, etc.)
            request.events().forEach(event -> {
                logger.debug("Analytics event: type={}, sessionId={}, timestamp={}",
                    event.type(), event.sessionId(), event.timestamp());
            });

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to process analytics events", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public record AnalyticsRequest(List<AnalyticsEvent> events) {}
}