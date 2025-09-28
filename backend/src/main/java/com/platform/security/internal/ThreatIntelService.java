package com.platform.security.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing threat intelligence and external threat correlation.
 * Provides threat indicator management and correlation with security events.
 *
 * Constitutional Compliance:
 * - Performance: <200ms API response times with Redis caching
 * - Real-time: <1s threat correlation for security event enrichment
 * - Observability: All threat intel operations logged with correlation IDs
 * - Security: Threat data encrypted at rest and in transit
 */
@Service
@Validated
@Transactional
public class ThreatIntelService {

    private static final Logger log = LoggerFactory.getLogger(ThreatIntelService.class);

    private final ThreatIndicatorRepository threatIndicatorRepository;
    private final SecurityEventRepository securityEventRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ThreatIntelService(
            ThreatIndicatorRepository threatIndicatorRepository,
            SecurityEventRepository securityEventRepository,
            ApplicationEventPublisher eventPublisher) {
        this.threatIndicatorRepository = threatIndicatorRepository;
        this.securityEventRepository = securityEventRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create or update a threat indicator from external threat intelligence sources.
     * Handles deduplication and confidence scoring.
     */
    @Transactional
    public ThreatIndicator createOrUpdateThreatIndicator(@Valid @NotNull ThreatIndicator indicator) {
        log.info("Creating/updating threat indicator: type={}, value={}, source={}, confidence={}",
                indicator.getIndicatorType(), indicator.getIndicatorValue(), indicator.getSource(), indicator.getConfidence());

        // Check for existing indicator
        var existing = threatIndicatorRepository.findActiveByTypeAndValue(
                indicator.getIndicatorType(), indicator.getIndicatorValue());

        if (existing.isPresent()) {
            var existingIndicator = existing.get();

            // Update with higher confidence or newer information
            if (indicator.getConfidence() > existingIndicator.getConfidence()) {
                existingIndicator.setConfidence(indicator.getConfidence());
            }

            // Update last seen timestamp
            existingIndicator.recordDetection();
            var savedIndicator = threatIndicatorRepository.save(existingIndicator);

            // Publish update event
            publishThreatIndicatorUpdated(savedIndicator);

            log.info("Threat indicator updated: id={}, type={}, value={}",
                    savedIndicator.getId(), savedIndicator.getIndicatorType(), savedIndicator.getIndicatorValue());

            return savedIndicator;
        } else {
            // Create new indicator
            var savedIndicator = threatIndicatorRepository.save(indicator);

            // Publish creation event
            publishThreatIndicatorCreated(savedIndicator);

            // Check for immediate correlations with recent security events
            correlateWithRecentEvents(savedIndicator);

            log.info("Threat indicator created: id={}, type={}, value={}",
                    savedIndicator.getId(), savedIndicator.getIndicatorType(), savedIndicator.getIndicatorValue());

            return savedIndicator;
        }
    }

    /**
     * Get threat indicator by ID with caching.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "threat-indicators", key = "#indicatorId")
    public Optional<ThreatIndicator> getThreatIndicator(@NonNull UUID indicatorId) {
        log.debug("Retrieving threat indicator: id={}", indicatorId);

        var indicator = threatIndicatorRepository.findById(indicatorId);
        if (indicator.isPresent()) {
            log.debug("Threat indicator found: id={}, type={}, confidence={}",
                    indicatorId, indicator.get().getIndicatorType(), indicator.get().getConfidence());
        } else {
            log.warn("Threat indicator not found: id={}", indicatorId);
        }

        return indicator;
    }

    /**
     * Search threat indicators by type and value for real-time correlation.
     * Used during security event processing for immediate threat assessment.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "threat-lookup", key = "#type + '-' + #value")
    public Optional<ThreatIndicator> lookupThreatIndicator(@NonNull ThreatIndicator.IndicatorType type, @NonNull String value) {
        log.debug("Looking up threat indicator: type={}, value={}", type, value);

        var indicator = threatIndicatorRepository.findActiveByTypeAndValue(type, value);

        if (indicator.isPresent()) {
            log.debug("Threat indicator found: id={}, confidence={}, active={}",
                    indicator.get().getId(), indicator.get().getConfidence(), indicator.get().getActive());
        } else {
            log.debug("No threat indicator found for: type={}, value={}", type, value);
        }

        return indicator;
    }

    /**
     * Find threat indicators with complex filtering.
     * Used for dashboard display and threat hunting.
     */
    @Transactional(readOnly = true)
    public List<ThreatIndicator> findThreatIndicators(
            ThreatIndicator.IndicatorType indicatorType,
            ThreatIndicator.Severity severity,
            ThreatIndicator.ThreatType threatType,
            Integer minConfidence,
            Set<String> tags,
            String source,
            Boolean active,
            @Positive int page,
            @Positive int size) {

        log.debug("Finding threat indicators: indicatorType={}, severity={}, threatType={}, minConfidence={}, tags={}, source={}, active={}, page={}, size={}",
                indicatorType, severity, threatType, minConfidence, tags, source, active, page, size);

        // Create pageable
        Pageable pageable = PageRequest.of(page, Math.min(size, 1000),
                Sort.by(Sort.Direction.DESC, "confidence", "lastSeen"));

        List<ThreatIndicator> indicators;
        if (tags != null && !tags.isEmpty()) {
            indicators = threatIndicatorRepository.findByTags(tags, pageable).getContent();
        } else if (indicatorType != null && severity != null && threatType != null && source != null) {
            indicators = threatIndicatorRepository.findWithFilters(
                    indicatorType, severity, threatType, source, active, pageable).getContent();
        } else if (threatType != null) {
            indicators = threatIndicatorRepository.findByThreatType(threatType, pageable).getContent();
        } else if (severity != null) {
            indicators = threatIndicatorRepository.findBySeverity(severity, pageable).getContent();
        } else if (source != null) {
            indicators = threatIndicatorRepository.findBySource(source, pageable).getContent();
        } else {
            indicators = threatIndicatorRepository.findAll(pageable).getContent();
        }

        log.debug("Found {} threat indicators", indicators.size());

        return indicators;
    }

    /**
     * Check if indicator value should be blocked based on threat intelligence.
     */
    @Transactional(readOnly = true)
    public boolean shouldBlockIndicator(@NonNull ThreatIndicator.IndicatorType type, @NonNull String value) {
        log.debug("Checking if indicator should be blocked: type={}, value={}", type, value);

        var shouldBlock = threatIndicatorRepository.shouldBlock(type, value);

        log.debug("Block decision for {}: {}", value, shouldBlock);

        return shouldBlock;
    }

    /**
     * Update threat indicator status and properties.
     */
    @Transactional
    public ThreatIndicator updateThreatIndicator(@NonNull UUID indicatorId,
                                               boolean active,
                                               Double confidence,
                                               ThreatIndicator.ListStatus listStatus) {
        log.info("Updating threat indicator: id={}, active={}, confidence={}, listStatus={}",
                indicatorId, active, confidence, listStatus);

        var indicator = threatIndicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Threat indicator not found: " + indicatorId));

        if (confidence != null) {
            indicator.setConfidence(confidence);
        }

        if (listStatus != null) {
            indicator.setListStatus(listStatus);
        }

        indicator.setActive(active);

        var savedIndicator = threatIndicatorRepository.save(indicator);

        publishThreatIndicatorUpdated(savedIndicator);

        log.info("Threat indicator updated successfully: id={}", savedIndicator.getId());

        return savedIndicator;
    }

    /**
     * Delete threat indicator.
     */
    @Transactional
    public void deleteThreatIndicator(@NonNull UUID indicatorId) {
        log.info("Deleting threat indicator: id={}", indicatorId);

        if (!threatIndicatorRepository.existsById(indicatorId)) {
            throw new IllegalArgumentException("Threat indicator not found: " + indicatorId);
        }

        threatIndicatorRepository.deleteById(indicatorId);

        log.info("Threat indicator deleted successfully: id={}", indicatorId);
    }

    /**
     * Correlate security event with threat intelligence.
     * Enriches security events with threat context for enhanced analysis.
     */
    @Transactional(readOnly = true)
    public ThreatCorrelationResult correlateSecurityEvent(@NonNull SecurityEvent event) {
        log.debug("Correlating security event with threat intel: eventId={}, sourceIp={}",
                event.getId(), event.getSourceIp());

        var correlations = List.<ThreatIndicator>of();

        // Check IP address correlation
        if (event.getSourceIp() != null) {
            var ipThreat = lookupThreatIndicator(ThreatIndicator.IndicatorType.IP_ADDRESS, event.getSourceIp());
            if (ipThreat.isPresent()) {
                correlations = List.of(ipThreat.get());
            }
        }

        // Check user agent correlation if available
        if (event.getUserAgent() != null) {
            var uaThreat = lookupThreatIndicator(ThreatIndicator.IndicatorType.USER_AGENT, event.getUserAgent());
            if (uaThreat.isPresent()) {
                correlations = correlations.isEmpty() ?
                    List.of(uaThreat.get()) :
                    List.<ThreatIndicator>of(correlations.get(0), uaThreat.get());
            }
        }

        var maxThreatLevel = correlations.stream()
                .mapToDouble(ThreatIndicator::getConfidence)
                .max()
                .orElse(0.0);

        var result = new ThreatCorrelationResult(
                event.getId(),
                correlations,
                maxThreatLevel,
                !correlations.isEmpty(),
                Instant.now()
        );

        if (!correlations.isEmpty()) {
            log.info("Threat correlation found: eventId={}, threats={}, maxThreatLevel={}",
                    event.getId(), correlations.size(), maxThreatLevel);

            // Publish correlation event for alerting
            publishThreatCorrelation(result);
        } else {
            log.debug("No threat correlations found for event: {}", event.getId());
        }

        return result;
    }

    /**
     * Mark threat indicator as false positive.
     * Updates threat intelligence database and prevents future alerts.
     */
    @Transactional
    public ThreatIndicator markAsFalsePositive(@NonNull UUID indicatorId, @NonNull String reason) {
        log.info("Marking threat indicator as false positive: id={}, reason={}", indicatorId, reason);

        var indicator = threatIndicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Threat indicator not found: " + indicatorId));

        indicator.recordFalsePositive();
        indicator.setActive(false);

        var savedIndicator = threatIndicatorRepository.save(indicator);

        // Publish false positive event
        publishFalsePositiveMarked(savedIndicator, reason);

        log.info("Threat indicator marked as false positive: id={}", savedIndicator.getId());

        return savedIndicator;
    }

    /**
     * Get threat intelligence statistics for dashboard display.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "threat-statistics", key = "'all'")
    public Map<String, Object> getThreatStatistics() {
        log.debug("Calculating threat intelligence statistics");

        var totalIndicators = threatIndicatorRepository.count();
        var highSeverityCount = threatIndicatorRepository.findHighSeverityIndicators(
                PageRequest.of(0, 1)).getTotalElements();

        var typeDistribution = threatIndicatorRepository.getThreatTypeDistribution();
        var severityDistribution = threatIndicatorRepository.getSeverityDistribution();
        var sourceStats = threatIndicatorRepository.getSourceReliabilityStats();

        var recentIndicators = threatIndicatorRepository.findTopDetectedSince(
                Instant.now().minusSeconds(24 * 60 * 60), // Last 24 hours
                PageRequest.of(0, 10));

        var statistics = Map.of(
                "totalIndicators", totalIndicators,
                "highSeverityIndicators", highSeverityCount,
                "recentIndicators", recentIndicators.size(),
                "typeDistribution", typeDistribution,
                "severityDistribution", severityDistribution,
                "sourceStats", sourceStats,
                "lastUpdated", Instant.now()
        );

        log.debug("Threat statistics: total={}, highSeverity={}, recent={}",
                totalIndicators, highSeverityCount, recentIndicators.size());

        return statistics;
    }

    /**
     * Clean up old threat indicators based on retention policy.
     */
    @Transactional
    public int cleanupOldThreatIndicators(Instant cutoffTime) {
        log.info("Cleaning up old threat indicators: cutoff={}", cutoffTime);

        var deletedCount = threatIndicatorRepository.deleteOldInactiveIndicators(cutoffTime);

        log.info("Deleted {} old threat indicators", deletedCount);

        return deletedCount;
    }

    /**
     * Import threat indicators from external feed.
     */
    @Transactional
    public ImportResult importThreatFeed(@NonNull List<ThreatIndicator> indicators, @NonNull String source) {
        log.info("Importing threat feed: source={}, count={}", source, indicators.size());

        var imported = 0;
        var updated = 0;
        var skipped = 0;

        for (var indicator : indicators) {
            try {
                var existing = threatIndicatorRepository.findActiveByTypeAndValue(
                        indicator.getIndicatorType(), indicator.getIndicatorValue());

                if (existing.isPresent()) {
                    // Update existing if confidence is higher
                    if (indicator.getConfidence() > existing.get().getConfidence()) {
                        createOrUpdateThreatIndicator(indicator);
                        updated++;
                    } else {
                        skipped++;
                    }
                } else {
                    // Create new indicator
                    createOrUpdateThreatIndicator(indicator);
                    imported++;
                }
            } catch (Exception e) {
                log.warn("Failed to import threat indicator: type={}, value={}",
                        indicator.getIndicatorType(), indicator.getIndicatorValue(), e);
                skipped++;
            }
        }

        var result = new ImportResult(imported, updated, skipped, source, Instant.now());

        log.info("Threat feed import completed: source={}, imported={}, updated={}, skipped={}",
                source, imported, updated, skipped);

        return result;
    }

    // Helper methods

    private void correlateWithRecentEvents(ThreatIndicator indicator) {
        // Look for recent security events that match this threat indicator
        var recentEvents = securityEventRepository.findByTimestampAfter(
                Instant.now().minusSeconds(60 * 60), // Last hour
                PageRequest.of(0, 100));

        for (var event : recentEvents) {
            var correlation = correlateSecurityEvent(event);
            if (correlation.hasThreats()) {
                log.info("Retroactive threat correlation found: eventId={}, indicatorId={}",
                        event.getId(), indicator.getId());
            }
        }
    }

    // Event publishing methods

    private void publishThreatIndicatorCreated(ThreatIndicator indicator) {
        try {
            var event = new ThreatIndicatorCreatedEvent(
                    indicator.getId(),
                    indicator.getIndicatorType(),
                    indicator.getConfidence(),
                    indicator.getSource()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish ThreatIndicatorCreated event: id={}", indicator.getId(), e);
        }
    }

    private void publishThreatIndicatorUpdated(ThreatIndicator indicator) {
        try {
            var event = new ThreatIndicatorUpdatedEvent(
                    indicator.getId(),
                    indicator.getConfidence(),
                    indicator.getLastSeen()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish ThreatIndicatorUpdated event: id={}", indicator.getId(), e);
        }
    }

    private void publishThreatCorrelation(ThreatCorrelationResult correlation) {
        try {
            var event = new ThreatCorrelationEvent(
                    correlation.eventId(),
                    correlation.threats().stream().map(ThreatIndicator::getId).collect(Collectors.toList()),
                    correlation.maxThreatLevel(),
                    correlation.correlatedAt()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish ThreatCorrelation event: eventId={}", correlation.eventId(), e);
        }
    }

    private void publishFalsePositiveMarked(ThreatIndicator indicator, String reason) {
        try {
            var event = new FalsePositiveMarkedEvent(
                    indicator.getId(),
                    indicator.getIndicatorType(),
                    indicator.getIndicatorValue(),
                    reason,
                    Instant.now()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish FalsePositiveMarked event: id={}", indicator.getId(), e);
        }
    }

    // Data transfer objects

    public record ThreatCorrelationResult(
            String eventId,
            List<ThreatIndicator> threats,
            Double maxThreatLevel,
            Boolean hasThreats,
            Instant correlatedAt
    ) {}

    public record ImportResult(
            Integer imported,
            Integer updated,
            Integer skipped,
            String source,
            Instant importedAt
    ) {}

    // Events

    public record ThreatIndicatorCreatedEvent(
            UUID indicatorId,
            ThreatIndicator.IndicatorType type,
            Double confidence,
            String source
    ) {}

    public record ThreatIndicatorUpdatedEvent(
            UUID indicatorId,
            Double confidence,
            Instant lastSeen
    ) {}

    public record ThreatCorrelationEvent(
            String eventId,
            List<UUID> threatIds,
            Double maxThreatLevel,
            Instant correlatedAt
    ) {}

    public record FalsePositiveMarkedEvent(
            UUID indicatorId,
            ThreatIndicator.IndicatorType type,
            String value,
            String reason,
            Instant markedAt
    ) {}
}