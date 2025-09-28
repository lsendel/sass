# Performance Monitoring Guide - RBAC Module

## Overview

The RBAC module has strict performance requirements with permission checks completing in under 200ms (99th percentile). This document outlines performance monitoring strategies, optimization techniques, and troubleshooting procedures to maintain optimal system performance.

## Performance Requirements

### Response Time Targets

| Operation | Target (95th percentile) | Target (99th percentile) | Alert Threshold |
|-----------|-------------------------|-------------------------|-----------------|
| Permission Check (Single) | < 50ms | < 100ms | > 200ms |
| Permission Check (Batch) | < 100ms | < 200ms | > 500ms |
| Role Assignment | < 200ms | < 500ms | > 1000ms |
| Role Creation | < 500ms | < 1000ms | > 2000ms |
| Cache Operations | < 10ms | < 25ms | > 50ms |

### Throughput Targets

| Operation | Target TPS | Peak TPS | Alert Threshold |
|-----------|------------|----------|-----------------|
| Permission Checks | 1000+ | 5000+ | CPU > 80% |
| Role Operations | 100+ | 500+ | Queue depth > 100 |
| User Assignments | 200+ | 1000+ | Error rate > 1% |

### Resource Utilization

| Resource | Normal | Peak | Alert Threshold |
|----------|--------|------|-----------------|
| CPU Usage | < 50% | < 80% | > 85% |
| Memory Usage | < 60% | < 85% | > 90% |
| Database Connections | < 70% | < 90% | > 95% |
| Cache Memory | < 75% | < 90% | > 95% |

## Monitoring Architecture

### Metrics Collection

#### Application Metrics (Micrometer)
```java
@Component
@Timed(name = "rbac.permission.check", description = "Permission check timing")
public class PermissionService {

    private final Counter permissionChecks;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Timer permissionCheckTimer;
    private final Gauge activeUsers;

    public PermissionService(MeterRegistry meterRegistry) {
        this.permissionChecks = Counter.builder("rbac.permission.checks.total")
            .description("Total permission checks performed")
            .register(meterRegistry);

        this.cacheHits = Counter.builder("rbac.cache.hits.total")
            .description("Cache hits for permission data")
            .register(meterRegistry);

        this.cacheMisses = Counter.builder("rbac.cache.misses.total")
            .description("Cache misses requiring database lookup")
            .register(meterRegistry);

        this.permissionCheckTimer = Timer.builder("rbac.permission.check.duration")
            .description("Permission check execution time")
            .register(meterRegistry);

        this.activeUsers = Gauge.builder("rbac.users.active")
            .description("Currently active users with cached permissions")
            .register(meterRegistry, this, PermissionService::getActiveUserCount);
    }
}
```

#### Custom Metrics Configuration
```yaml
management:
  metrics:
    tags:
      application: platform-rbac
      environment: ${ENVIRONMENT:dev}
    export:
      prometheus:
        enabled: true
        step: 10s
    distribution:
      percentiles-histogram:
        http.server.requests: true
        rbac.permission.check.duration: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
        rbac.permission.check.duration: 0.5, 0.95, 0.99
      sla:
        http.server.requests: 50ms, 100ms, 200ms, 500ms
        rbac.permission.check.duration: 10ms, 50ms, 100ms, 200ms
```

### Database Performance Monitoring

#### PostgreSQL Metrics
```sql
-- Slow query monitoring
SELECT query, mean_time, calls, total_time
FROM pg_stat_statements
WHERE query LIKE '%permission%' OR query LIKE '%role%'
ORDER BY mean_time DESC
LIMIT 10;

-- Index usage analysis
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- Connection monitoring
SELECT
    datname,
    usename,
    application_name,
    state,
    count(*)
FROM pg_stat_activity
WHERE datname = 'platform'
GROUP BY datname, usename, application_name, state;

-- Cache hit ratio
SELECT
    'index hit rate' as name,
    (sum(idx_blks_hit)) / nullif(sum(idx_blks_hit + idx_blks_read),0) * 100 as ratio
FROM pg_stat_user_indexes
UNION ALL
SELECT
    'table hit rate' as name,
    sum(heap_blks_hit) / nullif(sum(heap_blks_hit) + sum(heap_blks_read),0) * 100 as ratio
FROM pg_stat_user_tables;
```

#### Database Performance Queries
```sql
-- Top slow queries affecting RBAC
CREATE VIEW rbac_slow_queries AS
SELECT
    query,
    calls,
    total_time,
    mean_time,
    min_time,
    max_time,
    stddev_time,
    (total_time / sum(total_time) OVER ()) * 100 as percentage
FROM pg_stat_statements
WHERE query LIKE ANY(ARRAY['%permissions%', '%roles%', '%user_roles%', '%role_permissions%'])
ORDER BY mean_time DESC;

-- Lock monitoring for RBAC tables
CREATE VIEW rbac_locks AS
SELECT
    pl.pid,
    psa.usename,
    pl.mode,
    pl.locktype,
    pl.relation::regclass,
    psa.query,
    psa.state,
    now() - psa.query_start as duration
FROM pg_locks pl
JOIN pg_stat_activity psa ON pl.pid = psa.pid
WHERE pl.relation::regclass::text IN ('permissions', 'roles', 'user_roles', 'role_permissions')
ORDER BY duration DESC;
```

### Redis Performance Monitoring

#### Redis Metrics Collection
```bash
#!/bin/bash
# redis-metrics.sh

REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD}

# Memory usage
echo "=== Redis Memory Usage ==="
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD info memory | grep -E "used_memory|used_memory_human|maxmemory|mem_fragmentation_ratio"

# Key statistics
echo "=== Redis Key Statistics ==="
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD info keyspace

# Performance statistics
echo "=== Redis Performance ==="
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD info stats | grep -E "total_commands_processed|instantaneous_ops_per_sec|hit_rate|miss_rate"

# Slow log
echo "=== Redis Slow Log ==="
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD slowlog get 10

# RBAC-specific key analysis
echo "=== RBAC Cache Analysis ==="
redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD --scan --pattern "rbac:*" | wc -l
echo "Total RBAC cache keys"

redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD --scan --pattern "rbac:user_permissions:*" | head -5 | xargs redis-cli -h $REDIS_HOST -p $REDIS_PORT -a $REDIS_PASSWORD ttl
echo "Sample TTL values for user permissions"
```

### Application Performance Monitoring

#### JVM Metrics
```yaml
management:
  metrics:
    enable:
      jvm: true
      process: true
      system: true
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,threaddump,heapdump
```

#### Custom Performance Monitoring
```java
@Component
public class RbacPerformanceMonitor {

    private final MeterRegistry meterRegistry;
    private final Timer.Sample permissionCheckSample;

    @EventListener
    public void handlePermissionCheck(PermissionCheckEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);

        // Record permission check metrics
        meterRegistry.counter("rbac.permission.checks",
            "resource", event.getResource(),
            "action", event.getAction(),
            "result", event.isGranted() ? "granted" : "denied")
            .increment();

        // Record timing when check completes
        sample.stop(Timer.builder("rbac.permission.check.duration")
            .tag("resource", event.getResource())
            .register(meterRegistry));
    }

    @EventListener
    public void handleCacheOperation(CacheOperationEvent event) {
        meterRegistry.counter("rbac.cache.operations",
            "operation", event.getOperation(),
            "cache", event.getCacheName(),
            "result", event.isHit() ? "hit" : "miss")
            .increment();
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void recordCacheStatistics() {
        // Record cache size metrics
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCacheManager.CaffeineCache) {
                com.github.benmanes.caffeine.cache.stats.CacheStats stats =
                    ((CaffeineCacheManager.CaffeineCache) cache).getNativeCache().stats();

                meterRegistry.gauge("rbac.cache.size",
                    Tags.of("cache", cacheName), stats.requestCount());
                meterRegistry.gauge("rbac.cache.hit.ratio",
                    Tags.of("cache", cacheName), stats.hitRate());
            }
        });
    }
}
```

## Performance Optimization

### Database Optimization

#### Index Strategy
```sql
-- Core performance indexes for RBAC queries
-- (These should already exist from migrations)

-- Permission lookup optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_permissions_resource_action_active
ON permissions (resource, action) WHERE is_active = true;

-- User permission lookup optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_roles_user_active
ON user_roles (user_id)
WHERE removed_at IS NULL AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP);

-- Role permission lookup optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_role_permissions_role
ON role_permissions (role_id);

-- Organization role lookup optimization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_roles_org_active
ON roles (organization_id) WHERE is_active = true;

-- Composite index for user permission queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_permissions_composite
ON user_roles (user_id, role_id)
WHERE removed_at IS NULL AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP);
```

#### Query Optimization
```sql
-- Optimized user permission query using database function
CREATE OR REPLACE FUNCTION get_user_permissions_optimized(p_user_id BIGINT, p_organization_id BIGINT)
RETURNS TABLE(resource VARCHAR, action VARCHAR) AS $$
BEGIN
    -- Use materialized view or optimized joins
    RETURN QUERY
    WITH user_active_roles AS (
        SELECT DISTINCT ur.role_id
        FROM user_roles ur
        JOIN roles r ON ur.role_id = r.id
        WHERE ur.user_id = p_user_id
          AND r.organization_id = p_organization_id
          AND r.is_active = true
          AND ur.removed_at IS NULL
          AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
    )
    SELECT DISTINCT p.resource, p.action
    FROM permissions p
    JOIN role_permissions rp ON p.id = rp.permission_id
    JOIN user_active_roles uar ON rp.role_id = uar.role_id
    WHERE p.is_active = true;
END;
$$ LANGUAGE plpgsql STABLE;

-- Create materialized view for frequent lookups (if needed)
CREATE MATERIALIZED VIEW user_permission_cache AS
SELECT
    ur.user_id,
    r.organization_id,
    p.resource,
    p.action,
    r.id as role_id,
    r.name as role_name
FROM user_roles ur
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE ur.removed_at IS NULL
  AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)
  AND r.is_active = true
  AND p.is_active = true;

-- Create unique index on materialized view
CREATE UNIQUE INDEX idx_user_permission_cache_unique
ON user_permission_cache (user_id, organization_id, resource, action);

-- Refresh procedure (called by scheduled job)
CREATE OR REPLACE FUNCTION refresh_user_permission_cache()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY user_permission_cache;
END;
$$ LANGUAGE plpgsql;
```

#### Connection Pool Optimization
```yaml
spring:
  datasource:
    hikari:
      # Connection pool sizing
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 600000      # 10 minutes
      max-lifetime: 1800000     # 30 minutes
      connection-timeout: 30000  # 30 seconds

      # Performance optimization
      leak-detection-threshold: 60000  # 1 minute
      initialization-fail-timeout: 1
      isolate-internal-queries: false
      allow-pool-suspension: false
      read-only: false
      register-mbeans: true

      # Connection properties
      connection-test-query: SELECT 1
      connection-init-sql: SET SESSION synchronous_commit = off

      # Pool name for monitoring
      pool-name: RBACConnectionPool
```

### Cache Optimization

#### Cache Configuration Tuning
```yaml
spring:
  cache:
    type: redis
    redis:
      # Optimize for RBAC access patterns
      time-to-live: 900s  # 15 minutes
      cache-null-values: false
      key-prefix: "rbac:"
      use-key-prefix: true

  data:
    redis:
      # Connection optimization
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: -1ms
          time-between-eviction-runs: 30s
        shutdown-timeout: 100ms

      # Serialization optimization
      serialization: jdk  # Consider switching to JSON for better debugging

      # Connection timeout optimization
      timeout: 2000ms
      connect-timeout: 2000ms
      command-timeout: 5000ms
```

#### Cache Strategy Optimization
```java
@Service
public class OptimizedPermissionService {

    @Cacheable(value = "userPermissions", key = "#userId + ':' + #organizationId",
               unless = "#result.isEmpty()")
    public Set<String> getUserPermissionKeys(Long userId, Long organizationId) {
        return permissionRepository.findByUserId(userId, organizationId)
            .stream()
            .map(Permission::getPermissionKey)
            .collect(Collectors.toSet());
    }

    @Cacheable(value = "batchPermissionCheck", key = "#userId + ':' + #organizationId + ':' + #permissions.hashCode()")
    public List<PermissionCheckResult> checkUserPermissionsBatch(
            Long userId, Long organizationId, List<PermissionCheckRequest> permissions) {

        Set<String> userPermissions = getUserPermissionKeys(userId, organizationId);

        return permissions.parallelStream()
            .map(req -> new PermissionCheckResult(
                req.getResource(),
                req.getAction(),
                userPermissions.contains(req.getResource() + ":" + req.getAction())
            ))
            .collect(Collectors.toList());
    }
}
```

### Application Optimization

#### Async Processing
```java
@Configuration
@EnableAsync
public class RbacAsyncConfiguration {

    @Bean("rbacTaskExecutor")
    public TaskExecutor rbacTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("rbac-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("eventExecutor")
    public TaskExecutor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("rbac-event-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

#### Batch Processing Optimization
```java
@Service
public class BatchPermissionService {

    private static final int MAX_BATCH_SIZE = 100;

    public List<PermissionCheckResult> checkPermissionsBatch(
            Long userId, Long organizationId, List<PermissionCheckRequest> requests) {

        if (requests.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size exceeds maximum: " + MAX_BATCH_SIZE);
        }

        // Single cache lookup for user permissions
        Set<String> userPermissions = getUserPermissionKeys(userId, organizationId);

        // Process all requests in parallel
        return requests.parallelStream()
            .map(request -> checkSinglePermission(userPermissions, request))
            .collect(Collectors.toList());
    }

    private PermissionCheckResult checkSinglePermission(
            Set<String> userPermissions, PermissionCheckRequest request) {
        String permissionKey = request.getResource() + ":" + request.getAction();
        return new PermissionCheckResult(
            request.getResource(),
            request.getAction(),
            userPermissions.contains(permissionKey)
        );
    }
}
```

## Performance Testing

### Load Testing Strategy

#### JMeter Test Plan
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.name">RBAC Performance Test</stringProp>
      <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
        <collectionProp name="Arguments.arguments">
          <elementProp name="" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">http://localhost:8080</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup>
        <stringProp name="ThreadGroup.name">Permission Check Load Test</stringProp>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.path">/api/organizations/1/permissions/check</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <stringProp name="HTTPSampler.postBodyRaw">{"permissions":[{"resource":"PAYMENTS","action":"READ"}]}</stringProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

#### Performance Test Scenarios
```bash
#!/bin/bash
# performance-test.sh

BASE_URL="http://localhost:8080"
AUTH_TOKEN="your-jwt-token-here"

echo "=== RBAC Performance Test Suite ==="

# Single permission check test
echo "Testing single permission check latency..."
for i in {1..1000}; do
    curl -s -w "%{time_total}\n" -o /dev/null \
        -H "Authorization: Bearer $AUTH_TOKEN" \
        -H "Content-Type: application/json" \
        -X GET "$BASE_URL/api/organizations/1/permissions/check?resource=PAYMENTS&action=READ"
done | awk '{sum+=$1; count++} END {print "Average:", sum/count*1000, "ms"}'

# Batch permission check test
echo "Testing batch permission check latency..."
BATCH_PAYLOAD='{"permissions":[
    {"resource":"PAYMENTS","action":"READ"},
    {"resource":"PAYMENTS","action":"WRITE"},
    {"resource":"USERS","action":"READ"},
    {"resource":"USERS","action":"WRITE"},
    {"resource":"ORGANIZATIONS","action":"READ"}
]}'

for i in {1..500}; do
    curl -s -w "%{time_total}\n" -o /dev/null \
        -H "Authorization: Bearer $AUTH_TOKEN" \
        -H "Content-Type: application/json" \
        -X POST "$BASE_URL/api/organizations/1/permissions/check" \
        -d "$BATCH_PAYLOAD"
done | awk '{sum+=$1; count++} END {print "Average:", sum/count*1000, "ms"}'

# Role management test
echo "Testing role operations latency..."
ROLE_PAYLOAD='{"name":"test-role","description":"Test role","permissionIds":[1,2,3]}'

for i in {1..100}; do
    curl -s -w "%{time_total}\n" -o /dev/null \
        -H "Authorization: Bearer $AUTH_TOKEN" \
        -H "Content-Type: application/json" \
        -X POST "$BASE_URL/api/organizations/1/roles" \
        -d "$ROLE_PAYLOAD"
done | awk '{sum+=$1; count++} END {print "Average:", sum/count*1000, "ms"}'
```

### Continuous Performance Testing

#### Automated Performance Pipeline
```yaml
# .github/workflows/performance-test.yml
name: Performance Test

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  performance-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: platform_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
    - uses: actions/checkout@v3

    - name: Set up Java 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Build application
      run: ./gradlew build -x test

    - name: Start application
      run: |
        java -jar build/libs/*.jar &
        sleep 30  # Wait for startup

    - name: Run performance tests
      run: |
        chmod +x scripts/performance-test.sh
        scripts/performance-test.sh > performance-results.txt

    - name: Validate performance requirements
      run: |
        # Check that 95% of requests are under 200ms
        python scripts/validate-performance.py performance-results.txt

    - name: Upload performance results
      uses: actions/upload-artifact@v3
      with:
        name: performance-results
        path: performance-results.txt
```

## Performance Dashboards

### Prometheus Queries

#### Key Performance Indicators
```promql
# Permission check latency (95th percentile)
histogram_quantile(0.95,
  rate(rbac_permission_check_duration_seconds_bucket[5m])
)

# Cache hit ratio
(
  rate(rbac_cache_hits_total[5m]) /
  (rate(rbac_cache_hits_total[5m]) + rate(rbac_cache_misses_total[5m]))
) * 100

# Throughput (requests per second)
rate(rbac_permission_checks_total[1m])

# Error rate
(
  rate(http_requests_total{job="platform-rbac",status=~"5.."}[5m]) /
  rate(http_requests_total{job="platform-rbac"}[5m])
) * 100

# Database connection pool utilization
(
  hikaricp_connections_active{pool="RBACConnectionPool"} /
  hikaricp_connections_max{pool="RBACConnectionPool"}
) * 100

# JVM memory utilization
(
  jvm_memory_used_bytes{area="heap"} /
  jvm_memory_max_bytes{area="heap"}
) * 100
```

### Grafana Dashboard Configuration
```json
{
  "dashboard": {
    "id": null,
    "title": "RBAC Performance Dashboard",
    "tags": ["rbac", "performance"],
    "timezone": "browser",
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "id": 1,
        "title": "Permission Check Latency",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(rbac_permission_check_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile",
            "refId": "A"
          },
          {
            "expr": "histogram_quantile(0.99, rate(rbac_permission_check_duration_seconds_bucket[5m]))",
            "legendFormat": "99th percentile",
            "refId": "B"
          }
        ],
        "yAxes": [
          {
            "label": "Time (seconds)",
            "max": 0.5,
            "min": 0
          }
        ],
        "thresholds": [
          {
            "value": 0.2,
            "colorMode": "critical",
            "op": "gt"
          }
        ]
      },
      {
        "id": 2,
        "title": "Cache Performance",
        "type": "singlestat",
        "targets": [
          {
            "expr": "(rate(rbac_cache_hits_total[5m]) / (rate(rbac_cache_hits_total[5m]) + rate(rbac_cache_misses_total[5m]))) * 100",
            "refId": "A"
          }
        ],
        "valueName": "current",
        "format": "percent",
        "thresholds": "80,90",
        "colorBackground": true
      },
      {
        "id": 3,
        "title": "Request Throughput",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(rbac_permission_checks_total[1m])",
            "legendFormat": "Permission Checks/sec",
            "refId": "A"
          },
          {
            "expr": "rate(rbac_role_operations_total[1m])",
            "legendFormat": "Role Operations/sec",
            "refId": "B"
          }
        ]
      }
    ]
  }
}
```

## Performance Troubleshooting

### Common Performance Issues

#### Slow Permission Checks
```bash
# Diagnose slow permission checks

# 1. Check cache hit ratio
curl -s http://localhost:8080/actuator/metrics/rbac.cache.hit.ratio

# 2. Check database query performance
psql -d platform -c "
SELECT query, mean_time, calls
FROM pg_stat_statements
WHERE query LIKE '%get_user_permissions%'
ORDER BY mean_time DESC;
"

# 3. Check connection pool status
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active

# 4. Monitor Redis latency
redis-cli --latency-history -h redis-host -p 6379

# 5. Check application metrics
curl -s http://localhost:8080/actuator/metrics/rbac.permission.check.duration
```

#### High Memory Usage
```bash
# Memory analysis

# 1. Check JVM memory usage
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used

# 2. Generate heap dump for analysis
curl -X POST http://localhost:8080/actuator/heapdump

# 3. Check cache memory usage
redis-cli info memory

# 4. Monitor garbage collection
curl -s http://localhost:8080/actuator/metrics/jvm.gc.pause
```

#### Database Performance Issues
```sql
-- Database performance analysis

-- Check for blocking queries
SELECT
    bl.pid as blocked_pid,
    ka.query as blocked_query,
    bl.mode as blocked_mode,
    a.pid as blocking_pid,
    ka2.query as blocking_query,
    a.mode as blocking_mode
FROM pg_locks bl
JOIN pg_stat_activity ka ON bl.pid = ka.pid
JOIN pg_locks a ON a.granted = true
JOIN pg_stat_activity ka2 ON a.pid = ka2.pid
WHERE NOT bl.granted
  AND bl.relation = a.relation;

-- Check index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) as size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
  AND (idx_scan = 0 OR idx_scan < 100)
ORDER BY pg_relation_size(indexrelid) DESC;

-- Check table statistics
SELECT
    schemaname,
    tablename,
    n_tup_ins,
    n_tup_upd,
    n_tup_del,
    n_live_tup,
    n_dead_tup,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY n_dead_tup DESC;
```

### Performance Optimization Checklist

#### Application Level
- [ ] Cache hit ratio > 90%
- [ ] Permission checks < 200ms (99th percentile)
- [ ] Batch processing for multiple permission checks
- [ ] Async processing for non-critical operations
- [ ] Connection pooling properly configured
- [ ] JVM tuning parameters applied

#### Database Level
- [ ] All necessary indexes created
- [ ] Query execution plans optimized
- [ ] Connection pool sizing appropriate
- [ ] Database statistics up to date
- [ ] Slow query monitoring enabled
- [ ] Regular maintenance tasks scheduled

#### Infrastructure Level
- [ ] Adequate CPU and memory resources
- [ ] SSD storage for database
- [ ] Low latency network between components
- [ ] Monitoring and alerting configured
- [ ] Load balancing if multiple instances
- [ ] Resource limits configured in Kubernetes

## Support and Escalation

### Performance Team Contacts
- **Performance Engineering**: performance@company.com
- **Database Team**: dba@company.com
- **Infrastructure Team**: infrastructure@company.com
- **RBAC Development Team**: rbac-dev@company.com

### Escalation Procedures
1. **L1 Support**: Check standard metrics and logs
2. **L2 Support**: Deep dive into performance analysis
3. **L3 Support**: Architecture and code-level optimization
4. **L4 Support**: Infrastructure and scaling decisions

---

**Document Version**: 1.0
**Last Updated**: 2024-01-15
**Next Review**: 2024-04-15