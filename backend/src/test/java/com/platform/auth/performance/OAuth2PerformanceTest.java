package com.platform.auth.performance;

import com.platform.auth.api.OAuth2Controller;
import com.platform.auth.internal.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance tests for OAuth2 operations ensuring response times meet SLA requirements.
 *
 * Performance targets:
 * - Provider endpoint: < 50ms (p99)
 * - Authorization initiation: < 100ms (p99)
 * - Session validation: < 30ms (p99)
 * - User info retrieval: < 50ms (p99)
 * - Concurrent operations: No degradation up to 100 concurrent requests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OAuth2PerformanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OAuth2UserService userService;

    @Autowired
    private OAuth2SessionService sessionService;

    @Autowired
    private OAuth2AuditService auditService;

    @Autowired
    private OAuth2ProvidersService providersService;

    @Autowired
    private OAuth2UserInfoRepository userInfoRepository;

    @Autowired
    private OAuth2SessionRepository sessionRepository;

    private static final int WARMUP_ITERATIONS = 10;
    private static final int TEST_ITERATIONS = 100;
    private static final int CONCURRENT_USERS = 100;
    private static final Duration P99_THRESHOLD = Duration.ofMillis(50);
    private static final Duration P95_THRESHOLD = Duration.ofMillis(30);

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/auth/oauth2";

        // Warmup - ensure JVM optimizations are applied
        performWarmup();
    }

    @Test
    @Order(1)
    @DisplayName("Provider endpoint should respond within 50ms (p99)")
    void testProviderEndpointPerformance() {
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.nanoTime();

            ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/providers", Map.class);

            long responseTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            responseTimes.add(responseTime);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        // Calculate percentiles
        Collections.sort(responseTimes);
        long p99 = responseTimes.get((int)(responseTimes.size() * 0.99));
        long p95 = responseTimes.get((int)(responseTimes.size() * 0.95));
        long p50 = responseTimes.get(responseTimes.size() / 2);

        // Log performance metrics
        System.out.printf("Provider Endpoint Performance:%n");
        System.out.printf("  P50: %dms%n", p50);
        System.out.printf("  P95: %dms%n", p95);
        System.out.printf("  P99: %dms%n", p99);
        System.out.printf("  Min: %dms%n", responseTimes.get(0));
        System.out.printf("  Max: %dms%n", responseTimes.get(responseTimes.size() - 1));

        // Assert performance requirements
        assertThat(p99).isLessThanOrEqualTo(P99_THRESHOLD.toMillis())
            .withFailMessage("P99 response time %dms exceeds threshold %dms",
                p99, P99_THRESHOLD.toMillis());

        assertThat(p95).isLessThanOrEqualTo(P95_THRESHOLD.toMillis())
            .withFailMessage("P95 response time %dms exceeds threshold %dms",
                p95, P95_THRESHOLD.toMillis());
    }

    @Test
    @Order(2)
    @DisplayName("Authorization initialization should respond within 100ms (p99)")
    void testAuthorizationInitPerformance() {
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.nanoTime();

            ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/authorize/google", Map.class);

            long responseTime = (System.nanoTime() - startTime) / 1_000_000;
            responseTimes.add(responseTime);

            assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.FOUND);
        }

        Collections.sort(responseTimes);
        long p99 = responseTimes.get((int)(responseTimes.size() * 0.99));
        long p95 = responseTimes.get((int)(responseTimes.size() * 0.95));

        System.out.printf("Authorization Init Performance:%n");
        System.out.printf("  P95: %dms%n", p95);
        System.out.printf("  P99: %dms%n", p99);

        assertThat(p99).isLessThanOrEqualTo(100)
            .withFailMessage("Authorization init P99 %dms exceeds 100ms threshold", p99);
    }

    @Test
    @Order(3)
    @DisplayName("Session validation should respond within 30ms (p99)")
    @Transactional
    void testSessionValidationPerformance() {
        // Create test sessions for performance testing
        List<String> sessionIds = createTestSessions(10);
        List<Long> responseTimes = new ArrayList<>();

        for (String sessionId : sessionIds) {
            for (int i = 0; i < 10; i++) {
                long startTime = System.nanoTime();

                Optional<OAuth2SessionService.OAuth2SessionInfo> sessionInfo =
                    sessionService.getSessionInfo(sessionId);

                long responseTime = (System.nanoTime() - startTime) / 1_000_000;
                responseTimes.add(responseTime);

                assertThat(sessionInfo).isPresent();
            }
        }

        Collections.sort(responseTimes);
        long p99 = responseTimes.get((int)(responseTimes.size() * 0.99));
        long p95 = responseTimes.get((int)(responseTimes.size() * 0.95));

        System.out.printf("Session Validation Performance:%n");
        System.out.printf("  P95: %dms%n", p95);
        System.out.printf("  P99: %dms%n", p99);

        assertThat(p99).isLessThanOrEqualTo(30)
            .withFailMessage("Session validation P99 %dms exceeds 30ms threshold", p99);
    }

    @Test
    @Order(4)
    @DisplayName("User info retrieval should respond within 50ms (p99)")
    @Transactional
    void testUserInfoRetrievalPerformance() {
        // Create test users
        List<OAuth2UserInfo> testUsers = createTestUsers(20);
        List<Long> responseTimes = new ArrayList<>();

        for (OAuth2UserInfo user : testUsers) {
            for (int i = 0; i < 5; i++) {
                long startTime = System.nanoTime();

                Optional<OAuth2UserInfo> userInfo = userInfoRepository
                    .findByProviderUserIdAndProvider(user.getProviderUserId(), user.getProvider());

                long responseTime = (System.nanoTime() - startTime) / 1_000_000;
                responseTimes.add(responseTime);

                assertThat(userInfo).isPresent();
            }
        }

        Collections.sort(responseTimes);
        long p99 = responseTimes.get((int)(responseTimes.size() * 0.99));
        long p95 = responseTimes.get((int)(responseTimes.size() * 0.95));

        System.out.printf("User Info Retrieval Performance:%n");
        System.out.printf("  P95: %dms%n", p95);
        System.out.printf("  P99: %dms%n", p99);

        assertThat(p99).isLessThanOrEqualTo(50)
            .withFailMessage("User info retrieval P99 %dms exceeds 50ms threshold", p99);
    }

    @Test
    @Order(5)
    @DisplayName("Concurrent provider requests should not degrade performance")
    void testConcurrentProviderRequests() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(CONCURRENT_USERS);
        List<Future<Long>> futures = new ArrayList<>();

        // Submit concurrent requests
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            Future<Long> future = executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    long startTime = System.nanoTime();
                    ResponseEntity<Map> response = restTemplate.getForEntity(
                        baseUrl + "/providers", Map.class);
                    long responseTime = (System.nanoTime() - startTime) / 1_000_000;

                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    return responseTime;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    completeLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Start all threads simultaneously
        long overallStart = System.nanoTime();
        startLatch.countDown();

        // Wait for all to complete with timeout
        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
        long overallTime = (System.nanoTime() - overallStart) / 1_000_000;

        assertThat(completed).isTrue()
            .withFailMessage("Concurrent requests did not complete within timeout");

        // Collect results
        List<Long> responseTimes = new ArrayList<>();
        for (Future<Long> future : futures) {
            responseTimes.add(future.get());
        }

        Collections.sort(responseTimes);
        long p99 = responseTimes.get((int)(responseTimes.size() * 0.99));
        long p95 = responseTimes.get((int)(responseTimes.size() * 0.95));
        long p50 = responseTimes.get(responseTimes.size() / 2);

        System.out.printf("Concurrent Request Performance (%d users):%n", CONCURRENT_USERS);
        System.out.printf("  Overall time: %dms%n", overallTime);
        System.out.printf("  P50: %dms%n", p50);
        System.out.printf("  P95: %dms%n", p95);
        System.out.printf("  P99: %dms%n", p99);
        System.out.printf("  Throughput: %.2f req/s%n",
            (CONCURRENT_USERS * 1000.0) / overallTime);

        // Even under load, p99 should stay reasonable (allow 2x normal threshold)
        assertThat(p99).isLessThanOrEqualTo(P99_THRESHOLD.toMillis() * 2)
            .withFailMessage("P99 under load %dms exceeds acceptable degradation", p99);

        executor.shutdown();
    }

    @Test
    @Order(6)
    @DisplayName("Audit logging should have minimal performance impact")
    @Transactional
    void testAuditLoggingPerformance() {
        List<Long> withoutAuditTimes = new ArrayList<>();
        List<Long> withAuditTimes = new ArrayList<>();

        // Test without audit logging (direct service call)
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.nanoTime();

            // Direct service call without audit
            Optional<OAuth2UserInfo> user = userInfoRepository
                .findByProviderUserIdAndProvider("test-user-" + i, "google");

            long responseTime = (System.nanoTime() - startTime) / 1_000_000;
            withoutAuditTimes.add(responseTime);
        }

        // Test with audit logging
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.nanoTime();

            // Service call with audit logging
            auditService.logUserLogin("google", "test-user-" + i,
                "session-" + i, "127.0.0.1", "Test Agent");

            long responseTime = (System.nanoTime() - startTime) / 1_000_000;
            withAuditTimes.add(responseTime);
        }

        Collections.sort(withoutAuditTimes);
        Collections.sort(withAuditTimes);

        double avgWithout = withoutAuditTimes.stream().mapToLong(Long::valueOf).average().orElse(0);
        double avgWith = withAuditTimes.stream().mapToLong(Long::valueOf).average().orElse(0);
        double overhead = ((avgWith - avgWithout) / avgWithout) * 100;

        System.out.printf("Audit Logging Performance Impact:%n");
        System.out.printf("  Without audit avg: %.2fms%n", avgWithout);
        System.out.printf("  With audit avg: %.2fms%n", avgWith);
        System.out.printf("  Overhead: %.2f%%%n", overhead);

        // Audit logging should add minimal overhead (< 20%)
        assertThat(overhead).isLessThanOrEqualTo(20.0)
            .withFailMessage("Audit logging overhead %.2f%% exceeds 20%% threshold", overhead);
    }

    @Test
    @Order(7)
    @DisplayName("Database query optimization verification")
    void testDatabaseQueryPerformance() {
        // This test would verify that database queries are optimized
        // In a real scenario, you might use Spring Boot Actuator metrics
        // or database query profiling tools

        List<Long> queryTimes = new ArrayList<>();

        // Test indexed queries
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            long startTime = System.nanoTime();

            // This should use the index on provider_user_id and provider
            userInfoRepository.findByProviderUserIdAndProvider("test-" + i, "google");

            long queryTime = (System.nanoTime() - startTime) / 1_000_000;
            queryTimes.add(queryTime);
        }

        Collections.sort(queryTimes);
        long p99 = queryTimes.get((int)(queryTimes.size() * 0.99));

        System.out.printf("Database Query Performance:%n");
        System.out.printf("  Indexed query P99: %dms%n", p99);

        assertThat(p99).isLessThanOrEqualTo(10)
            .withFailMessage("Database query P99 %dms exceeds 10ms threshold", p99);
    }

    private void performWarmup() {
        // Warmup to ensure JVM optimizations
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            restTemplate.getForEntity(baseUrl + "/providers", Map.class);
        }
    }

    private List<String> createTestSessions(int count) {
        List<String> sessionIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            OAuth2UserInfo userInfo = createTestUser(i);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", "test-user-" + i);
            attributes.put("email", "test" + i + "@example.com");
            OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"), attributes, "sub");

            String sessionId = "perf-test-session-" + UUID.randomUUID();
            OAuth2SessionService.OAuth2SessionResult result = sessionService.createSession(
                sessionId, oauth2User, "google", "127.0.0.1", "Performance Test Agent"
            );

            if (result.success()) {
                sessionIds.add(sessionId);
            }
        }

        return sessionIds;
    }

    private List<OAuth2UserInfo> createTestUsers(int count) {
        return IntStream.range(0, count)
            .mapToObj(this::createTestUser)
            .toList();
    }

    private OAuth2UserInfo createTestUser(int index) {
        return userService.findOrCreateUserInfo(
            "perf-test-user-" + index,
            "google",
            "perftest" + index + "@example.com",
            "Perf Test User " + index,
            "Perf",
            "User" + index,
            null,
            "en",
            true,
            "{}"
        );
    }
}