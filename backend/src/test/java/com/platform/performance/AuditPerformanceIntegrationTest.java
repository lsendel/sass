package com.platform.performance;

import com.platform.audit.internal.AuditEvent;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.audit.internal.EnhancedAuditService;
import com.platform.shared.monitoring.PerformanceMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for audit system performance with real database operations.
 * Tests performance improvements from indexes, caching, and optimization strategies.
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:perftest;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.org.springframework.cache=DEBUG"
})
@Transactional
class AuditPerformanceIntegrationTest {

    @Autowired
    private EnhancedAuditService auditService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PerformanceMonitoringService performanceMonitoring;

    private UUID testOrganizationId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testOrganizationId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        // Create test data for performance testing
        createTestAuditEvents(1000); // Create 1000 test events
    }

    @Test
    void testAuditEventQueryPerformance() {
        // Test: Query performance should be under 100ms for paginated results
        Instant startTime = Instant.now();

        Page<AuditEvent> events = auditService.getUserAuditEvents(
            testOrganizationId, testUserId, 0, 20);

        Duration queryTime = Duration.between(startTime, Instant.now());

        assertThat(queryTime.toMillis())
            .as("User audit events query should complete within 100ms")
            .isLessThan(100);

        assertThat(events.getContent()).isNotEmpty();
        assertThat(events.getSize()).isEqualTo(20);
    }

    @Test
    void testBatchAuditEventCreation() {
        // Test: Batch creation should be significantly faster than individual inserts
        List<AuditEvent> batchEvents = createTestEventList(500);

        Instant batchStartTime = Instant.now();
        auditService.createAuditEventsBatch(batchEvents);
        Duration batchTime = Duration.between(batchStartTime, Instant.now());

        // Batch operation should complete within 2 seconds for 500 events
        assertThat(batchTime.toMillis())
            .as("Batch audit event creation should complete within 2 seconds")
            .isLessThan(2000);

        // Verify events were created
        List<AuditEvent> recentEvents = auditService.getRecentEvents(batchStartTime);
        assertThat(recentEvents.size()).isGreaterThanOrEqualTo(500);
    }

    @Test
    void testAdvancedSearchPerformance() {
        // Test: Advanced search with multiple criteria should be efficient
        Instant fromDate = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant toDate = Instant.now();

        Instant startTime = Instant.now();

        Page<AuditEvent> searchResults = auditService.searchAuditEvents(
            testUserId.toString(),
            List.of("LOGIN", "LOGOUT"),
            List.of("INFO", "WARN"),
            fromDate,
            toDate,
            null,
            null,
            PageRequest.of(0, 50)
        );

        Duration searchTime = Duration.between(startTime, Instant.now());

        assertThat(searchTime.toMillis())
            .as("Advanced search should complete within 200ms")
            .isLessThan(200);

        assertThat(searchResults).isNotNull();
    }

    @Test
    void testCachePerformance() {
        // Test: Cached queries should be significantly faster on subsequent calls
        Instant since = Instant.now().minus(30, ChronoUnit.MINUTES);

        // First call - will hit database
        Instant firstCallStart = Instant.now();
        List<AuditEvent> firstCall = auditService.getRecentEvents(since);
        Duration firstCallTime = Duration.between(firstCallStart, Instant.now());

        // Second call - should hit cache
        Instant secondCallStart = Instant.now();
        List<AuditEvent> secondCall = auditService.getRecentEvents(since);
        Duration secondCallTime = Duration.between(secondCallStart, Instant.now());

        // Cached call should be at least 50% faster
        assertThat(secondCallTime.toMillis())
            .as("Cached query should be significantly faster")
            .isLessThan(firstCallTime.toMillis() / 2);

        assertThat(firstCall).hasSameSizeAs(secondCall);
    }

    @Test
    void testSecurityAnalysisPerformance() {
        // Test: Security analysis should complete within reasonable time
        createSecurityTestEvents(100); // Create security-related events

        Instant startTime = Instant.now();

        EnhancedAuditService.SecurityAnalysisResult analysis =
            auditService.performSecurityAnalysis(testOrganizationId,
                Instant.now().minus(1, ChronoUnit.HOURS), 1);

        Duration analysisTime = Duration.between(startTime, Instant.now());

        assertThat(analysisTime.toMillis())
            .as("Security analysis should complete within 500ms")
            .isLessThan(500);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getRiskScore()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testTimeframeQueryPerformance() {
        // Test: Timeframe queries should leverage indexes effectively
        Instant fromDate = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant toDate = Instant.now().minus(1, ChronoUnit.HOURS);

        Instant startTime = Instant.now();

        List<AuditEvent> timeframeEvents = auditService.getUserAuditEventsByTimeframe(
            testUserId.toString(), fromDate, toDate);

        Duration queryTime = Duration.between(startTime, Instant.now());

        assertThat(queryTime.toMillis())
            .as("Timeframe query should complete within 150ms")
            .isLessThan(150);

        assertThat(timeframeEvents).isNotNull();
    }

    @Test
    void testPerformanceMonitoringIntegration() {
        // Test: Performance monitoring should capture metrics correctly
        Map<String, Long> initialMetrics = performanceMonitoring.getRealTimeMetrics();

        // Perform some audit operations
        auditService.getUserAuditEvents(testOrganizationId, testUserId, 0, 10);
        auditService.getRecentEvents(Instant.now().minus(5, ChronoUnit.MINUTES));

        // Verify metrics were updated
        Map<String, Long> updatedMetrics = performanceMonitoring.getRealTimeMetrics();

        assertThat(updatedMetrics.get("total_requests"))
            .as("Request count should increase")
            .isGreaterThan(initialMetrics.get("total_requests"));
    }

    @Test
    void testConcurrentAuditOperations() {
        // Test: Concurrent audit operations should maintain performance
        int threadCount = 10;
        int operationsPerThread = 20;

        List<CompletableFuture<Duration>> futures = new ArrayList<>();

        Instant overallStart = Instant.now();

        for (int i = 0; i < threadCount; i++) {
            CompletableFuture<Duration> future = CompletableFuture.supplyAsync(() -> {
                Instant threadStart = Instant.now();

                for (int j = 0; j < operationsPerThread; j++) {
                    auditService.getUserAuditEvents(testOrganizationId, testUserId, j % 5, 10);
                }

                return Duration.between(threadStart, Instant.now());
            });

            futures.add(future);
        }

        // Wait for all operations to complete
        List<Duration> threadTimes = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        Duration overallTime = Duration.between(overallStart, Instant.now());

        // All threads should complete within reasonable time
        assertThat(overallTime.toMillis())
            .as("Concurrent operations should complete within 5 seconds")
            .isLessThan(5000);

        // No individual thread should take more than 2 seconds
        threadTimes.forEach(threadTime ->
            assertThat(threadTime.toMillis())
                .as("Individual thread should complete within 2 seconds")
                .isLessThan(2000)
        );
    }

    @Test
    void testMemoryUsageDuringLargeOperations() {
        // Test: Memory usage should remain stable during large operations
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Perform large batch operation
        List<AuditEvent> largeEventList = createTestEventList(2000);
        auditService.createAuditEventsBatch(largeEventList);

        // Force garbage collection
        runtime.gc();
        Thread.sleep(100);

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        // Memory increase should be reasonable (less than 100MB)
        assertThat(memoryIncrease)
            .as("Memory increase should be less than 100MB")
            .isLessThan(100 * 1024 * 1024);
    }

    // Helper methods

    private void createTestAuditEvents(int count) {
        List<AuditEvent> events = createTestEventList(count);
        auditEventRepository.saveAll(events);
        auditEventRepository.flush();
    }

    private List<AuditEvent> createTestEventList(int count) {
        List<AuditEvent> events = new ArrayList<>();
        Instant baseTime = Instant.now().minus(2, ChronoUnit.HOURS);

        for (int i = 0; i < count; i++) {
            AuditEvent event = new AuditEvent();
            event.setId(UUID.randomUUID());
            event.setOrganizationId(testOrganizationId);
            event.setActorId(testUserId);
            event.setAction(getRandomAction(i));
            event.setResourceType("TEST_RESOURCE");
            event.setResourceId("resource-" + i);
            event.setIpAddress("192.168.1." + (i % 255));
            event.setCreatedAt(baseTime.plus(i, ChronoUnit.SECONDS));
            event.setCorrelationId("correlation-" + (i % 10));
            event.setDetails("Test event " + i);

            events.add(event);
        }

        return events;
    }

    private void createSecurityTestEvents(int count) {
        List<AuditEvent> securityEvents = new ArrayList<>();
        String[] securityActions = {"LOGIN_FAILED", "UNAUTHORIZED_ACCESS", "SUSPICIOUS_ACTIVITY"};

        for (int i = 0; i < count; i++) {
            AuditEvent event = new AuditEvent();
            event.setId(UUID.randomUUID());
            event.setOrganizationId(testOrganizationId);
            event.setActorId(UUID.randomUUID());
            event.setAction(securityActions[i % securityActions.length]);
            event.setIpAddress("10.0.0." + (i % 255));
            event.setCreatedAt(Instant.now().minus(i, ChronoUnit.MINUTES));
            event.setDetails("Security test event " + i);

            securityEvents.add(event);
        }

        auditEventRepository.saveAll(securityEvents);
        auditEventRepository.flush();
    }

    private String getRandomAction(int index) {
        String[] actions = {"LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE", "VIEW"};
        return actions[index % actions.length];
    }
}