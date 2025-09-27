package com.platform.performance;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.audit.internal.AuditEventRepository;
import com.platform.payment.internal.PaymentRepository;
import com.platform.shared.security.PlatformUserPrincipal;
import com.platform.shared.security.TenantContext;
import com.platform.subscription.internal.Plan;
import com.platform.subscription.internal.PlanRepository;
import com.platform.subscription.internal.SubscriptionRepository;
import com.platform.shared.types.Money;
import com.platform.user.internal.Organization;
import com.platform.user.internal.OrganizationRepository;
import com.platform.user.internal.User;
import com.platform.user.internal.UserRepository;

/**
 * Performance and load integration tests.
 * Tests system performance under concurrent load, response times, and throughput.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PerformanceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private UUID orgId;
    private UUID userId;
    private UUID planId;

    @BeforeEach
    void setUp() {
        // Clear existing data
        auditEventRepository.deleteAll();
        subscriptionRepository.deleteAll();
        paymentRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        planRepository.deleteAll();

        // Create test organization
        Organization org = new Organization("Performance Test Corp", "perf-test", (UUID) null);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Create test user
        User user = new User("perf@example.com", "Performance Test User");
        user.setOrganization(org);
        user = userRepository.save(user);
        userId = user.getId();

        // Create test plan
        Plan plan = new Plan("Performance Plan", "price_perf_test",
                           new Money(new BigDecimal("99.99"), "USD"),
                           Plan.BillingInterval.MONTH);
        setField(plan, "slug", "performance-plan");
        setField(plan, "active", true);
        plan = planRepository.save(plan);
        planId = plan.getId();

        // Set authentication context
        authenticateAs(userId, orgId, "perf-test", "USER");
    }

    @Test
    @Timeout(30)
    void shouldHandleConcurrentUserCreation() throws Exception {
        int numThreads = 20;
        int usersPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        // Create admin user for user creation
        authenticateAs(userId, orgId, "perf-test", "ADMIN");

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int i = 0; i < usersPerThread; i++) {
                        long startTime = System.currentTimeMillis();

                        String userRequest = String.format("""
                            {
                                "email": "perfuser%d_%d@example.com",
                                "name": "Performance User %d-%d",
                                "role": "USER"
                            }
                            """, threadId, i, threadId, i);

                        try {
                            MvcResult result = mockMvc.perform(post("/api/v1/organizations/{orgId}/users", orgId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(userRequest))
                                    .andExpect(status().isCreated())
                                    .andReturn();

                            long responseTime = System.currentTimeMillis() - startTime;
                            totalResponseTime.addAndGet(responseTime);
                            successCount.incrementAndGet();

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Error creating user: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        Instant testStart = Instant.now();
        startLatch.countDown(); // Start all threads
        assertTrue(completeLatch.await(25, TimeUnit.SECONDS));
        Instant testEnd = Instant.now();

        executor.shutdown();

        // Verify performance metrics
        int totalExpected = numThreads * usersPerThread;
        double successRate = (double) successCount.get() / totalExpected;
        double avgResponseTime = (double) totalResponseTime.get() / successCount.get();
        Duration totalDuration = Duration.between(testStart, testEnd);

        System.out.printf("Concurrent User Creation Results:%n");
        System.out.printf("  Total operations: %d%n", totalExpected);
        System.out.printf("  Successful: %d%n", successCount.get());
        System.out.printf("  Failed: %d%n", errorCount.get());
        System.out.printf("  Success rate: %.2f%%%n", successRate * 100);
        System.out.printf("  Average response time: %.2f ms%n", avgResponseTime);
        System.out.printf("  Total test duration: %d ms%n", totalDuration.toMillis());
        System.out.printf("  Throughput: %.2f ops/sec%n",
                         (double) successCount.get() / totalDuration.toSeconds());

        // Assert performance criteria
        assertTrue(successRate >= 0.95, "Success rate should be at least 95%");
        assertTrue(avgResponseTime <= 1000, "Average response time should be under 1 second");
        assertTrue(totalDuration.toSeconds() <= 20, "Total test should complete within 20 seconds");
    }

    @Test
    @Timeout(30)
    void shouldHandleConcurrentPaymentProcessing() throws Exception {
        // First create payment methods for testing
        List<String> paymentMethodIds = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String paymentMethodRequest = String.format("""
                {
                    "stripePaymentMethodId": "pm_test_concurrent_%d",
                    "type": "CARD",
                    "billingDetails": {
                        "name": "Concurrent Test User %d",
                        "email": "perf@example.com"
                    },
                    "cardDetails": {
                        "brand": "visa",
                        "lastFour": "424%d",
                        "expMonth": 12,
                        "expYear": 2025
                    }
                }
                """, i, i, i % 10);

            var result = mockMvc.perform(post("/api/v1/organizations/{orgId}/payment-methods", orgId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentMethodRequest))
                    .andExpect(status().isCreated())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            var pmResponse = objectMapper.readTree(responseJson);
            paymentMethodIds.add(pmResponse.get("id").asText());
        }

        int numThreads = 15;
        int paymentsPerThread = 3;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int i = 0; i < paymentsPerThread; i++) {
                        long startTime = System.currentTimeMillis();

                        String paymentMethodId = paymentMethodIds.get((threadId + i) % paymentMethodIds.size());
                        double amount = 10.00 + (threadId * 10) + i;

                        String paymentRequest = String.format("""
                            {
                                "amount": %.2f,
                                "currency": "usd",
                                "description": "Concurrent payment %d-%d",
                                "paymentMethodId": "%s"
                            }
                            """, amount, threadId, i, paymentMethodId);

                        try {
                            mockMvc.perform(post("/api/v1/organizations/{orgId}/payments", orgId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(paymentRequest))
                                    .andExpect(status().isCreated());

                            long responseTime = System.currentTimeMillis() - startTime;
                            totalResponseTime.addAndGet(responseTime);
                            successCount.incrementAndGet();

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Error processing payment: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        Instant testStart = Instant.now();
        startLatch.countDown();
        assertTrue(completeLatch.await(25, TimeUnit.SECONDS));
        Instant testEnd = Instant.now();

        executor.shutdown();

        // Verify performance metrics
        int totalExpected = numThreads * paymentsPerThread;
        double successRate = (double) successCount.get() / totalExpected;
        double avgResponseTime = (double) totalResponseTime.get() / Math.max(successCount.get(), 1);
        Duration totalDuration = Duration.between(testStart, testEnd);

        System.out.printf("Concurrent Payment Processing Results:%n");
        System.out.printf("  Total payments: %d%n", totalExpected);
        System.out.printf("  Successful: %d%n", successCount.get());
        System.out.printf("  Failed: %d%n", errorCount.get());
        System.out.printf("  Success rate: %.2f%%%n", successRate * 100);
        System.out.printf("  Average response time: %.2f ms%n", avgResponseTime);
        System.out.printf("  Total test duration: %d ms%n", totalDuration.toMillis());
        System.out.printf("  Throughput: %.2f payments/sec%n",
                         (double) successCount.get() / totalDuration.toSeconds());

        // Assert performance criteria
        assertTrue(successRate >= 0.90, "Payment success rate should be at least 90%");
        assertTrue(avgResponseTime <= 2000, "Average payment response time should be under 2 seconds");
    }

    @Test
    @Timeout(20)
    void shouldHandleHighVolumeAuditLogQueries() throws Exception {
        // Create large number of audit events
        int numEvents = 1000;

        Instant startCreation = Instant.now();
        for (int i = 0; i < numEvents; i++) {
            String eventType = (i % 3 == 0) ? "USER_LOGIN" :
                              (i % 3 == 1) ? "PAYMENT_PROCESSED" : "RESOURCE_ACCESS";

            // Direct repository save for performance (bypass async processing)
            var auditEvent = new com.platform.audit.internal.AuditEvent(
                orgId, userId, eventType, "RESOURCE", "resource-" + i,
                "ACTION", "192.168.1." + (i % 255), "TestAgent"
            );
            auditEventRepository.save(auditEvent);
        }
        Instant endCreation = Instant.now();

        System.out.printf("Created %d audit events in %d ms%n",
                         numEvents, Duration.between(startCreation, endCreation).toMillis());

        // Test concurrent audit queries
        int numQueryThreads = 10;
        int queriesPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numQueryThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numQueryThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalQueryTime = new AtomicLong(0);

        for (int t = 0; t < numQueryThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int i = 0; i < queriesPerThread; i++) {
                        long startTime = System.currentTimeMillis();

                        try {
                            // Test different query patterns
                            switch (i % 4) {
                                case 0:
                                    // Organization audit events (paginated)
                                    mockMvc.perform(get("/api/v1/organizations/{orgId}/audit", orgId)
                                            .param("page", String.valueOf(threadId % 10))
                                            .param("size", "50")
                                            .contentType(MediaType.APPLICATION_JSON))
                                            .andExpect(status().isOk());
                                    break;

                                case 1:
                                    // User-specific audit events
                                    mockMvc.perform(get("/api/v1/organizations/{orgId}/audit/users/{userId}", orgId, userId)
                                            .param("page", "0")
                                            .param("size", "20")
                                            .contentType(MediaType.APPLICATION_JSON))
                                            .andExpect(status().isOk());
                                    break;

                                case 2:
                                    // Event type filtering
                                    mockMvc.perform(get("/api/v1/organizations/{orgId}/audit", orgId)
                                            .param("eventType", "USER_LOGIN")
                                            .param("page", "0")
                                            .param("size", "25")
                                            .contentType(MediaType.APPLICATION_JSON))
                                            .andExpect(status().isOk());
                                    break;

                                case 3:
                                    // Date range queries
                                    mockMvc.perform(get("/api/v1/organizations/{orgId}/audit", orgId)
                                            .param("startDate", Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS).toString())
                                            .param("endDate", Instant.now().toString())
                                            .param("page", "0")
                                            .param("size", "30")
                                            .contentType(MediaType.APPLICATION_JSON))
                                            .andExpect(status().isOk());
                                    break;
                            }

                            long queryTime = System.currentTimeMillis() - startTime;
                            totalQueryTime.addAndGet(queryTime);
                            successCount.incrementAndGet();

                        } catch (Exception e) {
                            System.err.println("Query error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        Instant testStart = Instant.now();
        startLatch.countDown();
        assertTrue(completeLatch.await(15, TimeUnit.SECONDS));
        Instant testEnd = Instant.now();

        executor.shutdown();

        // Verify query performance
        int totalQueries = numQueryThreads * queriesPerThread;
        double avgQueryTime = (double) totalQueryTime.get() / Math.max(successCount.get(), 1);
        Duration totalDuration = Duration.between(testStart, testEnd);

        System.out.printf("High Volume Audit Query Results:%n");
        System.out.printf("  Total queries: %d%n", totalQueries);
        System.out.printf("  Successful: %d%n", successCount.get());
        System.out.printf("  Average query time: %.2f ms%n", avgQueryTime);
        System.out.printf("  Total test duration: %d ms%n", totalDuration.toMillis());
        System.out.printf("  Query throughput: %.2f queries/sec%n",
                         (double) successCount.get() / totalDuration.toSeconds());

        // Assert performance criteria
        assertTrue(avgQueryTime <= 500, "Average query time should be under 500ms");
        assertTrue(successCount.get() >= totalQueries * 0.95, "95% of queries should succeed");
    }

    @Test
    @Timeout(25)
    void shouldMaintainPerformanceUnderMemoryPressure() throws Exception {
        // Create memory pressure by processing large datasets
        int batchSize = 100;
        int numBatches = 10;

        List<Long> batchTimes = new ArrayList<>();

        for (int batch = 0; batch < numBatches; batch++) {
            Instant batchStart = Instant.now();

            // Create batch of users
            for (int i = 0; i < batchSize; i++) {
                String userRequest = String.format("""
                    {
                        "email": "memtest%d_%d@example.com",
                        "name": "Memory Test User %d-%d",
                        "role": "USER",
                        "metadata": {
                            "batch": %d,
                            "index": %d,
                            "largeData": "%s"
                        }
                    }
                    """, batch, i, batch, i, batch, i, "x".repeat(1000)); // Add some bulk

                try {
                    mockMvc.perform(post("/api/v1/organizations/{orgId}/users", orgId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userRequest))
                            .andExpect(status().isCreated());
                } catch (Exception e) {
                    // Some failures expected under pressure
                }
            }

            Instant batchEnd = Instant.now();
            long batchTime = Duration.between(batchStart, batchEnd).toMillis();
            batchTimes.add(batchTime);

            System.out.printf("Batch %d completed in %d ms%n", batch + 1, batchTime);

            // Force garbage collection between batches
            System.gc();
            Thread.sleep(100);
        }

        // Analyze performance degradation
        double avgFirstHalf = batchTimes.subList(0, numBatches / 2).stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        double avgSecondHalf = batchTimes.subList(numBatches / 2, numBatches).stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        double degradationRatio = avgSecondHalf / avgFirstHalf;

        System.out.printf("Memory Pressure Test Results:%n");
        System.out.printf("  Average time first half: %.2f ms%n", avgFirstHalf);
        System.out.printf("  Average time second half: %.2f ms%n", avgSecondHalf);
        System.out.printf("  Performance degradation ratio: %.2f%n", degradationRatio);

        // Performance should not degrade significantly under memory pressure
        assertTrue(degradationRatio <= 2.0, "Performance degradation should be less than 2x");
        assertTrue(avgSecondHalf <= 5000, "Batch processing should stay under 5 seconds");
    }

    @Test
    @Timeout(20)
    void shouldHandleDatabaseConnectionPoolExhaustion() throws Exception {
        // Simulate many concurrent long-running database operations
        int numConnections = 30; // Likely to exceed default pool size
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numConnections);
        ExecutorService executor = Executors.newFixedThreadPool(numConnections);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);

        for (int i = 0; i < numConnections; i++) {
            final int connectionId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Simulate long-running query by creating subscription with complex operations
                    String subscriptionRequest = String.format("""
                        {
                            "planId": "%s",
                            "metadata": {
                                "connectionTest": %d,
                                "timestamp": "%s"
                            }
                        }
                        """, planId, connectionId, Instant.now().toString());

                    try {
                        mockMvc.perform(post("/api/v1/organizations/{orgId}/subscription", orgId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(subscriptionRequest))
                                .andExpect(status().isCreated());

                        successCount.incrementAndGet();

                    } catch (Exception e) {
                        if (e.getMessage().contains("timeout") || e.getMessage().contains("pool")) {
                            timeoutCount.incrementAndGet();
                        }
                        System.err.printf("Connection %d failed: %s%n", connectionId, e.getMessage());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        Instant testStart = Instant.now();
        startLatch.countDown();
        assertTrue(completeLatch.await(18, TimeUnit.SECONDS));
        Instant testEnd = Instant.now();

        executor.shutdown();

        Duration totalDuration = Duration.between(testStart, testEnd);
        double successRate = (double) successCount.get() / numConnections;

        System.out.printf("Database Connection Pool Test Results:%n");
        System.out.printf("  Total connections attempted: %d%n", numConnections);
        System.out.printf("  Successful: %d%n", successCount.get());
        System.out.printf("  Timeouts: %d%n", timeoutCount.get());
        System.out.printf("  Success rate: %.2f%%%n", successRate * 100);
        System.out.printf("  Total duration: %d ms%n", totalDuration.toMillis());

        // System should gracefully handle connection pool pressure
        assertTrue(successRate >= 0.70, "At least 70% of operations should succeed");
        assertTrue(totalDuration.toSeconds() <= 15, "Test should complete within 15 seconds");
    }

    @Test
    @Timeout(15)
    void shouldMaintainResponseTimesUnderLoad() throws Exception {
        // Warm up the system
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // Measure response times under increasing load
        int[] loadLevels = {1, 5, 10, 20};

        for (int loadLevel : loadLevels) {
            List<Long> responseTimes = new ArrayList<>();
            CountDownLatch completeLatch = new CountDownLatch(loadLevel);
            ExecutorService executor = Executors.newFixedThreadPool(loadLevel);

            Instant loadStart = Instant.now();

            for (int i = 0; i < loadLevel; i++) {
                executor.submit(() -> {
                    try {
                        long startTime = System.currentTimeMillis();

                        mockMvc.perform(get("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());

                        long responseTime = System.currentTimeMillis() - startTime;
                        synchronized (responseTimes) {
                            responseTimes.add(responseTime);
                        }

                    } catch (Exception e) {
                        System.err.println("Load test error: " + e.getMessage());
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }

            assertTrue(completeLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            Instant loadEnd = Instant.now();

            // Calculate statistics
            double avgResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            long maxResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);

            long totalLoadTime = Duration.between(loadStart, loadEnd).toMillis();

            System.out.printf("Load Level %d Results:%n", loadLevel);
            System.out.printf("  Average response time: %.2f ms%n", avgResponseTime);
            System.out.printf("  Max response time: %d ms%n", maxResponseTime);
            System.out.printf("  Total load duration: %d ms%n", totalLoadTime);
            System.out.printf("  Concurrent throughput: %.2f req/sec%n",
                             (double) loadLevel / (totalLoadTime / 1000.0));

            // Assert response time criteria
            assertTrue(avgResponseTime <= 200,
                      String.format("Average response time should be under 200ms at load %d", loadLevel));
            assertTrue(maxResponseTime <= 1000,
                      String.format("Max response time should be under 1000ms at load %d", loadLevel));
        }
    }

    private void authenticateAs(UUID userId, UUID orgId, String orgSlug, String role) {
        PlatformUserPrincipal principal = PlatformUserPrincipal.organizationMember(
            userId,
            "perf@example.com",
            "Performance Test User",
            orgId,
            orgSlug,
            role
        );

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        TenantContext.setTenantInfo(orgId, orgSlug, userId);
    }

    // Helper method for setting private fields via reflection
    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}