# Deployment Guide - RBAC Module

## Overview

This document provides comprehensive deployment instructions for the RBAC (Role-Based Access Control) module across different environments. The module requires specific database, cache, and security configurations to function properly.

## Prerequisites

### System Requirements

#### Production Environment
- **CPU**: 4+ cores, 2.4GHz+ per node
- **Memory**: 8GB+ RAM per application instance
- **Storage**: SSD storage for database with 1000+ IOPS
- **Network**: Low latency (<10ms) between app and database/cache

#### Development Environment
- **CPU**: 2+ cores
- **Memory**: 4GB+ RAM
- **Storage**: Standard SSD
- **Network**: Standard connectivity

### Technology Stack

#### Required Components
- **Java**: OpenJDK 21+ (LTS recommended)
- **Spring Boot**: 3.5.6+
- **PostgreSQL**: 15+ (with function support)
- **Redis**: 7+ (for caching and sessions)
- **Docker**: 24+ (for containerization)

#### Optional Components
- **Kubernetes**: 1.28+ (for orchestration)
- **Prometheus**: Metrics collection
- **Grafana**: Monitoring dashboards
- **ELK Stack**: Centralized logging

## Environment Configuration

### Environment Variables

#### Required Variables
```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/platform
DATABASE_USERNAME=platform_user
DATABASE_PASSWORD=${DB_PASSWORD}
DATABASE_POOL_SIZE=20

# Redis Configuration
REDIS_URL=redis://redis:6379
REDIS_PASSWORD=${REDIS_PASSWORD}
REDIS_POOL_SIZE=10

# Security Configuration
JWT_SECRET_KEY=${JWT_SECRET_KEY}
ENCRYPTION_KEY=${ENCRYPTION_KEY}

# RBAC Configuration
RBAC_MAX_ROLES_PER_ORG=50
RBAC_PERMISSION_CACHE_TTL=900
RBAC_AUDIT_ENABLED=true
```

#### Environment-Specific Variables

**Development**
```bash
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=DEBUG
RBAC_PERFORMANCE_MONITORING=false
RBAC_AUDIT_LOG_PERMISSION_CHECKS=true
```

**Staging**
```bash
SPRING_PROFILES_ACTIVE=staging
LOG_LEVEL=INFO
RBAC_PERFORMANCE_MONITORING=true
RBAC_AUDIT_LOG_PERMISSION_CHECKS=false
```

**Production**
```bash
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL=WARN
RBAC_PERFORMANCE_MONITORING=true
RBAC_AUDIT_LOG_PERMISSION_CHECKS=false
RBAC_STRICT_SECURITY=true
```

### Application Properties

#### application-prod.yml
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: ${DATABASE_POOL_SIZE:20}
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: ${REDIS_POOL_SIZE:10}
          max-idle: 8
          min-idle: 2
          max-wait: -1ms

  cache:
    type: redis
    redis:
      time-to-live: 900s
      key-prefix: "rbac:"
      cache-null-values: false

  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${JWT_JWK_SET_URI}

rbac:
  roles:
    max-custom-per-organization: ${RBAC_MAX_ROLES_PER_ORG:50}
    default-expiration-days: 90
  permissions:
    cache-ttl-minutes: ${RBAC_PERMISSION_CACHE_TTL:15}
    batch-check-limit: 100
  performance:
    monitoring-enabled: ${RBAC_PERFORMANCE_MONITORING:true}
    slow-query-threshold-ms: 200
  audit:
    enabled: ${RBAC_AUDIT_ENABLED:true}
    log-permission-checks: ${RBAC_AUDIT_LOG_PERMISSION_CHECKS:false}
    log-role-changes: true
    retention-days: 365
  security:
    strict-mode: ${RBAC_STRICT_SECURITY:true}
    rate-limiting-enabled: true
    max-failed-attempts: 5

logging:
  level:
    com.platform.user: ${LOG_LEVEL:INFO}
    org.springframework.security: WARN
    org.springframework.cache: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

## Database Setup

### PostgreSQL Configuration

#### Database Creation
```sql
-- Create database
CREATE DATABASE platform
  WITH ENCODING 'UTF8'
       LC_COLLATE = 'en_US.UTF-8'
       LC_CTYPE = 'en_US.UTF-8'
       TEMPLATE template0;

-- Create application user
CREATE USER platform_user WITH PASSWORD 'secure_password_here';

-- Grant necessary permissions
GRANT CONNECT ON DATABASE platform TO platform_user;
GRANT CREATE ON DATABASE platform TO platform_user;

-- Connect to the database
\c platform

-- Grant schema permissions
GRANT ALL PRIVILEGES ON SCHEMA public TO platform_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO platform_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO platform_user;

-- Set default privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO platform_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO platform_user;
```

#### Performance Tuning
```sql
-- PostgreSQL performance settings for RBAC workload
-- Add to postgresql.conf

# Memory Settings
shared_buffers = 2GB                    # 25% of system RAM
effective_cache_size = 6GB              # 75% of system RAM
work_mem = 16MB                         # For sorting operations
maintenance_work_mem = 512MB            # For maintenance operations

# Connection Settings
max_connections = 200                   # Adjust based on load
shared_preload_libraries = 'pg_stat_statements'

# Query Planning
random_page_cost = 1.1                  # For SSD storage
effective_io_concurrency = 200          # For SSD storage

# Write-Ahead Logging
wal_buffers = 64MB
checkpoint_completion_target = 0.9
checkpoint_timeout = 10min

# Logging
log_statement = 'mod'                   # Log all modifications
log_duration = on
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
log_checkpoints = on
log_connections = on
log_disconnections = on

# Statistics
track_activities = on
track_counts = on
track_functions = all
track_io_timing = on
```

### Migration Execution

#### Flyway Configuration
```yaml
flyway:
  url: ${DATABASE_URL}
  user: ${DATABASE_USERNAME}
  password: ${DATABASE_PASSWORD}
  locations: classpath:db/migration
  baseline-on-migrate: true
  validate-on-migrate: true
  clean-disabled: true
  schemas: public
```

#### Migration Execution
```bash
# Development environment
./gradlew flywayMigrate -Pflyway-dev

# Staging environment
./gradlew flywayMigrate -Pflyway-staging

# Production environment (with validation)
./gradlew flywayValidate -Pflyway-prod
./gradlew flywayMigrate -Pflyway-prod
```

#### Migration Verification
```sql
-- Check migration status
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- Verify RBAC tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('permissions', 'roles', 'user_roles', 'role_permissions');

-- Check system permissions are populated
SELECT COUNT(*) as permission_count FROM permissions WHERE is_active = true;
-- Should return 20 (4 resources × 4 actions + audit permissions)

-- Verify database functions
SELECT routine_name
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name IN ('get_user_permissions', 'invalidate_user_cache');
```

## Redis Configuration

### Redis Server Setup

#### redis.conf (Production)
```conf
# Network
bind 0.0.0.0
port 6379
protected-mode yes
requirepass your_redis_password_here

# General
daemonize yes
supervised systemd
pidfile /var/run/redis/redis-server.pid
loglevel notice
logfile /var/log/redis/redis-server.log

# Memory Management
maxmemory 4gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000

# Persistence
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# Security
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command CONFIG ""

# Performance
tcp-keepalive 300
timeout 0
tcp-backlog 511
databases 16

# RBAC-specific configuration
hash-max-ziplist-entries 512
hash-max-ziplist-value 64
list-max-ziplist-size -2
set-max-intset-entries 512
zset-max-ziplist-entries 128
zset-max-ziplist-value 64
```

### Redis Cluster (High Availability)

#### Cluster Configuration
```bash
# Redis Cluster with 6 nodes (3 masters, 3 replicas)

# Master nodes
redis-server --port 7000 --cluster-enabled yes --cluster-config-file nodes-7000.conf
redis-server --port 7001 --cluster-enabled yes --cluster-config-file nodes-7001.conf
redis-server --port 7002 --cluster-enabled yes --cluster-config-file nodes-7002.conf

# Replica nodes
redis-server --port 7003 --cluster-enabled yes --cluster-config-file nodes-7003.conf
redis-server --port 7004 --cluster-enabled yes --cluster-config-file nodes-7004.conf
redis-server --port 7005 --cluster-enabled yes --cluster-config-file nodes-7005.conf

# Create cluster
redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 \
          127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
          --cluster-replicas 1
```

## Docker Deployment

### Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Install necessary packages
RUN apt-get update && apt-get install -y \
    curl \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

# Copy application JAR
COPY build/libs/platform-backend-*.jar app.jar

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Docker Compose (Development)
```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DATABASE_URL=jdbc:postgresql://postgres:5432/platform
      - DATABASE_USERNAME=platform_user
      - DATABASE_PASSWORD=dev_password
      - REDIS_URL=redis://redis:6379
    depends_on:
      - postgres
      - redis
    networks:
      - platform-network

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=platform
      - POSTGRES_USER=platform_user
      - POSTGRES_PASSWORD=dev_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    ports:
      - "5432:5432"
    networks:
      - platform-network

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - platform-network

volumes:
  postgres_data:
  redis_data:

networks:
  platform-network:
    driver: bridge
```

## Kubernetes Deployment

### Namespace and RBAC
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: platform-rbac
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: platform-rbac-sa
  namespace: platform-rbac
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: platform-rbac
  name: platform-rbac-role
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: platform-rbac-binding
  namespace: platform-rbac
subjects:
- kind: ServiceAccount
  name: platform-rbac-sa
  namespace: platform-rbac
roleRef:
  kind: Role
  name: platform-rbac-role
  apiGroup: rbac.authorization.k8s.io
```

### ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: platform-rbac-config
  namespace: platform-rbac
data:
  application.yml: |
    spring:
      profiles:
        active: prod
    rbac:
      roles:
        max-custom-per-organization: 50
      permissions:
        cache-ttl-minutes: 15
      audit:
        enabled: true
        log-permission-checks: false
    logging:
      level:
        com.platform.user: INFO
```

### Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: platform-rbac-secrets
  namespace: platform-rbac
type: Opaque
data:
  database-password: <base64-encoded-password>
  redis-password: <base64-encoded-password>
  jwt-secret-key: <base64-encoded-jwt-secret>
  encryption-key: <base64-encoded-encryption-key>
```

### Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: platform-rbac
  namespace: platform-rbac
  labels:
    app: platform-rbac
spec:
  replicas: 3
  selector:
    matchLabels:
      app: platform-rbac
  template:
    metadata:
      labels:
        app: platform-rbac
    spec:
      serviceAccountName: platform-rbac-sa
      containers:
      - name: platform-rbac
        image: platform/rbac:latest
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DATABASE_URL
          value: "jdbc:postgresql://postgres-service:5432/platform"
        - name: DATABASE_USERNAME
          value: "platform_user"
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: platform-rbac-secrets
              key: database-password
        - name: REDIS_URL
          value: "redis://redis-service:6379"
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: platform-rbac-secrets
              key: redis-password
        - name: JWT_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: platform-rbac-secrets
              key: jwt-secret-key
        volumeMounts:
        - name: config
          mountPath: /app/config
          readOnly: true
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
      volumes:
      - name: config
        configMap:
          name: platform-rbac-config
      restartPolicy: Always
---
apiVersion: v1
kind: Service
metadata:
  name: platform-rbac-service
  namespace: platform-rbac
spec:
  selector:
    app: platform-rbac
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: platform-rbac-ingress
  namespace: platform-rbac
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - api.platform.com
    secretName: platform-rbac-tls
  rules:
  - host: api.platform.com
    http:
      paths:
      - path: /api/permissions
        pathType: Prefix
        backend:
          service:
            name: platform-rbac-service
            port:
              number: 80
      - path: /api/organizations/*/roles
        pathType: Prefix
        backend:
          service:
            name: platform-rbac-service
            port:
              number: 80
      - path: /api/organizations/*/users/*/roles
        pathType: Prefix
        backend:
          service:
            name: platform-rbac-service
            port:
              number: 80
```

### Horizontal Pod Autoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: platform-rbac-hpa
  namespace: platform-rbac
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: platform-rbac
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
```

## Monitoring and Observability

### Prometheus Configuration
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-rbac-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s

    scrape_configs:
    - job_name: 'platform-rbac'
      metrics_path: '/actuator/prometheus'
      static_configs:
      - targets: ['platform-rbac-service:80']
      scrape_interval: 10s

    rule_files:
    - "/etc/prometheus/rules/*.yml"

  rbac-alerts.yml: |
    groups:
    - name: rbac.rules
      rules:
      - alert: RBACHighErrorRate
        expr: rate(http_requests_total{job="platform-rbac",status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate in RBAC module"

      - alert: RBACSlowPermissionChecks
        expr: histogram_quantile(0.95, rate(rbac_permission_check_duration_seconds_bucket[5m])) > 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow permission checks detected"

      - alert: RBACCacheMissRate
        expr: rate(rbac_cache_misses_total[5m]) / rate(rbac_cache_requests_total[5m]) > 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High cache miss rate in RBAC"
```

### Grafana Dashboard
```json
{
  "dashboard": {
    "title": "RBAC Module Dashboard",
    "panels": [
      {
        "title": "Permission Check Latency",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(rbac_permission_check_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          },
          {
            "expr": "histogram_quantile(0.5, rate(rbac_permission_check_duration_seconds_bucket[5m]))",
            "legendFormat": "50th percentile"
          }
        ]
      },
      {
        "title": "Cache Hit Ratio",
        "type": "singlestat",
        "targets": [
          {
            "expr": "rate(rbac_cache_hits_total[5m]) / rate(rbac_cache_requests_total[5m]) * 100"
          }
        ]
      },
      {
        "title": "Permission Checks per Second",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(rbac_permission_checks_total[1m])"
          }
        ]
      },
      {
        "title": "Role Operations",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(rbac_role_assignments_total[5m])",
            "legendFormat": "Assignments"
          },
          {
            "expr": "rate(rbac_role_removals_total[5m])",
            "legendFormat": "Removals"
          }
        ]
      }
    ]
  }
}
```

## Backup and Recovery

### Database Backup Strategy

#### Automated Backups
```bash
#!/bin/bash
# backup-rbac-db.sh

BACKUP_DIR="/backups/rbac"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="platform"
DB_USER="platform_user"

# Create backup directory
mkdir -p $BACKUP_DIR

# Full database backup
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME \
  --verbose --clean --create --if-exists \
  --format=custom \
  --file="$BACKUP_DIR/rbac_full_$DATE.backup"

# Schema-only backup
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME \
  --schema-only --verbose \
  --format=plain \
  --file="$BACKUP_DIR/rbac_schema_$DATE.sql"

# Data-only backup for RBAC tables
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME \
  --data-only --verbose \
  --table=permissions --table=roles --table=user_roles --table=role_permissions \
  --format=custom \
  --file="$BACKUP_DIR/rbac_data_$DATE.backup"

# Cleanup old backups (keep 30 days)
find $BACKUP_DIR -name "*.backup" -mtime +30 -delete
find $BACKUP_DIR -name "*.sql" -mtime +30 -delete

echo "Backup completed: $DATE"
```

#### Recovery Procedures
```bash
#!/bin/bash
# restore-rbac-db.sh

BACKUP_FILE=$1
DB_NAME="platform"
DB_USER="platform_user"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup_file>"
    exit 1
fi

# Stop application
kubectl scale deployment platform-rbac --replicas=0

# Restore database
pg_restore -h $DB_HOST -U $DB_USER -d $DB_NAME \
  --verbose --clean --if-exists \
  --single-transaction \
  "$BACKUP_FILE"

# Verify restoration
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
SELECT COUNT(*) as permissions FROM permissions;
SELECT COUNT(*) as roles FROM roles;
SELECT COUNT(*) as user_roles FROM user_roles;
"

# Restart application
kubectl scale deployment platform-rbac --replicas=3

echo "Recovery completed"
```

### Redis Backup Strategy

#### Redis Backup
```bash
#!/bin/bash
# backup-redis.sh

BACKUP_DIR="/backups/redis"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Create Redis backup
redis-cli --rdb "$BACKUP_DIR/rbac_cache_$DATE.rdb"

# Cleanup old backups (keep 7 days for cache)
find $BACKUP_DIR -name "*.rdb" -mtime +7 -delete

echo "Redis backup completed: $DATE"
```

## Deployment Checklist

### Pre-Deployment

#### Environment Verification
- [ ] Java 21+ installed and configured
- [ ] PostgreSQL 15+ running and accessible
- [ ] Redis 7+ running and accessible
- [ ] Required environment variables set
- [ ] Network connectivity verified
- [ ] SSL certificates installed (production)

#### Database Preparation
- [ ] Database created with correct encoding
- [ ] Application user created with proper permissions
- [ ] Flyway migrations executed successfully
- [ ] System permissions populated
- [ ] Database performance tuning applied
- [ ] Backup strategy configured

#### Cache Preparation
- [ ] Redis server configured and secured
- [ ] Redis password set
- [ ] Memory limits configured
- [ ] Persistence settings configured
- [ ] Network access configured

### Deployment Execution

#### Application Deployment
- [ ] Application built with correct profile
- [ ] Docker image created and tested
- [ ] Kubernetes manifests applied
- [ ] Health checks passing
- [ ] Service endpoints accessible
- [ ] Ingress configured and working

#### Verification
- [ ] Database connectivity test
- [ ] Redis connectivity test
- [ ] Permission check API test
- [ ] Role management API test
- [ ] Authentication integration test
- [ ] Performance test (< 200ms permission checks)

### Post-Deployment

#### Monitoring Setup
- [ ] Prometheus scraping configured
- [ ] Grafana dashboards imported
- [ ] Alert rules configured
- [ ] Log aggregation configured
- [ ] Health check monitoring enabled

#### Security Validation
- [ ] SSL/TLS configuration verified
- [ ] Authentication working correctly
- [ ] Authorization boundaries tested
- [ ] Rate limiting functional
- [ ] Audit logging enabled

#### Operational Readiness
- [ ] Backup procedures tested
- [ ] Recovery procedures documented
- [ ] Monitoring alerts configured
- [ ] Runbook documentation complete
- [ ] Team training completed

## Troubleshooting

### Common Issues

#### Database Connection Issues
```bash
# Test database connectivity
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT version();"

# Check connection pool
curl -s http://localhost:8080/actuator/health | jq '.components.db'

# View active connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'platform';
```

#### Redis Connection Issues
```bash
# Test Redis connectivity
redis-cli -h $REDIS_HOST -p $REDIS_PORT ping

# Check cache health
curl -s http://localhost:8080/actuator/health | jq '.components.redis'

# Monitor Redis memory usage
redis-cli info memory
```

#### Performance Issues
```bash
# Check permission check latency
curl -s http://localhost:8080/actuator/metrics/rbac.permission.check.duration

# Monitor cache hit ratio
curl -s http://localhost:8080/actuator/metrics/rbac.cache.hit.ratio

# Check database query performance
SELECT query, mean_time, calls
FROM pg_stat_statements
WHERE query LIKE '%permissions%'
ORDER BY mean_time DESC;
```

### Log Analysis

#### Application Logs
```bash
# Search for permission errors
kubectl logs -f deployment/platform-rbac | grep "Permission denied"

# Monitor cache operations
kubectl logs -f deployment/platform-rbac | grep "Cache"

# Check authentication issues
kubectl logs -f deployment/platform-rbac | grep "Authentication"
```

#### Database Logs
```bash
# Monitor slow queries
tail -f /var/log/postgresql/postgresql-15-main.log | grep "duration"

# Check connection errors
tail -f /var/log/postgresql/postgresql-15-main.log | grep "ERROR"
```

## Support

### Contact Information
- **Development Team**: rbac-dev@company.com
- **Operations Team**: platform-ops@company.com
- **Security Team**: security@company.com
- **24/7 Support**: support@company.com

### Documentation Links
- [RBAC Module README](./README.md)
- [API Documentation](./api-docs/openapi.yaml)
- [Security Documentation](./SECURITY.md)
- [Architecture Decisions](./adr/)

---

**Document Version**: 1.0
**Last Updated**: 2024-01-15
**Next Review**: 2024-04-15