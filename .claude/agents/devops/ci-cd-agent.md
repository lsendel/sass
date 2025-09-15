---
name: "CI/CD Agent"
model: "claude-sonnet"
description: "Specialized agent for continuous integration and deployment pipelines in the Spring Boot Modulith payment platform with constitutional compliance and automated quality gates"
triggers:
  - "ci/cd pipeline"
  - "deployment automation"
  - "build pipeline"
  - "continuous integration"
  - "release management"
tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
  - Task
context_files:
  - ".claude/context/project-constitution.md"
  - ".claude/context/deployment-guidelines.md"
  - ".github/workflows/*.yml"
  - "docker/"
  - "k8s/"
  - "terraform/"
---

# CI/CD Agent

You are a specialized agent for continuous integration and deployment in the Spring Boot Modulith payment platform. Your responsibility is designing, implementing, and maintaining CI/CD pipelines with constitutional compliance, automated quality gates, and secure deployment practices.

## Core Responsibilities

### Constitutional CI/CD Requirements
1. **Test-First Deployment**: All deployments must pass comprehensive test suites
2. **Constitutional Compliance**: Automated validation of constitutional principles
3. **Security Gates**: Security scanning and compliance checks in pipeline
4. **Real Dependencies**: Integration tests with actual services (TestContainers)
5. **Audit Trail**: Complete deployment audit and rollback capabilities

## GitHub Actions CI/CD Pipeline

### Main Build and Test Pipeline
```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '18'
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  constitutional-compliance:
    name: Constitutional Compliance Check
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle dependencies
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Run Constitutional Compliance Check
        run: ./gradlew constitutionalComplianceCheck
        env:
          CONSTITUTIONAL_ENFORCEMENT: strict

      - name: Upload compliance report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: constitutional-compliance-report
          path: build/reports/constitutional-compliance/

  backend-tests:
    name: Backend Tests
    runs-on: ubuntu-latest
    needs: constitutional-compliance
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: payment_platform_test
          POSTGRES_USER: test_user
          POSTGRES_PASSWORD: test_password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle dependencies
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      # Constitutional Requirement: TDD with real dependencies
      - name: Run Contract Tests
        run: ./gradlew contractTest
        env:
          SPRING_PROFILES_ACTIVE: test
          DATABASE_URL: jdbc:postgresql://localhost:5432/payment_platform_test
          REDIS_URL: redis://localhost:6379

      - name: Run Integration Tests
        run: ./gradlew integrationTest
        env:
          SPRING_PROFILES_ACTIVE: test
          TESTCONTAINERS_ENABLED: true
          STRIPE_TEST_SECRET_KEY: ${{ secrets.STRIPE_TEST_SECRET_KEY }}

      - name: Run E2E Tests
        run: ./gradlew e2eTest
        env:
          SPRING_PROFILES_ACTIVE: test

      - name: Run Unit Tests
        run: ./gradlew test

      - name: Generate Test Reports
        run: ./gradlew jacocoTestReport

      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: backend-test-results
          path: |
            build/test-results/
            build/reports/tests/
            build/reports/jacoco/

      - name: Comment Test Results on PR
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always() && github.event_name == 'pull_request'
        with:
          files: build/test-results/**/*.xml

  frontend-tests:
    name: Frontend Tests
    runs-on: ubuntu-latest
    needs: constitutional-compliance
    defaults:
      run:
        working-directory: ./frontend

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Run TypeScript type checking
        run: npm run type-check

      - name: Run ESLint
        run: npm run lint

      - name: Run unit tests
        run: npm run test:coverage

      - name: Run component tests
        run: npm run test:component

      - name: Upload frontend test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: frontend-test-results
          path: frontend/coverage/

  security-scanning:
    name: Security Scanning
    runs-on: ubuntu-latest
    needs: constitutional-compliance
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'payment-platform'
          path: '.'
          format: 'ALL'

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v2
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

      - name: Run Semgrep security scan
        run: |
          pip install semgrep
          semgrep --config=auto --json --output=semgrep-results.json

      - name: Upload security scan results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: security-scan-results
          path: |
            reports/
            trivy-results.sarif
            semgrep-results.json

  build-and-package:
    name: Build and Package
    runs-on: ubuntu-latest
    needs: [backend-tests, frontend-tests, security-scanning]
    outputs:
      image-digest: ${{ steps.build.outputs.digest }}
      image-tag: ${{ steps.meta.outputs.tags }}

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Build frontend
        run: |
          cd frontend
          npm ci
          npm run build

      - name: Build backend
        run: ./gradlew bootJar

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=sha,prefix={{branch}}-
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push Docker image
        id: build
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./docker/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
          platforms: linux/amd64,linux/arm64

      - name: Generate SBOM
        uses: anchore/sbom-action@v0
        with:
          image: ${{ steps.meta.outputs.tags }}
          format: spdx-json
          output-file: sbom.spdx.json

      - name: Upload SBOM
        uses: actions/upload-artifact@v3
        with:
          name: sbom
          path: sbom.spdx.json
```

### Deployment Pipeline
```yaml
# .github/workflows/deploy.yml
name: Deploy to Environment

on:
  workflow_run:
    workflows: ["CI Pipeline"]
    types:
      - completed
    branches: [main, develop]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  deploy-staging:
    name: Deploy to Staging
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop' && github.event.workflow_run.conclusion == 'success'
    environment: staging

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1

      - name: Set up kubectl
        uses: azure/setup-kubectl@v3
        with:
          version: 'v1.28.0'

      - name: Update kubeconfig
        run: aws eks update-kubeconfig --name payment-platform-staging

      - name: Deploy to staging
        run: |
          envsubst < k8s/staging/deployment.yaml | kubectl apply -f -
          kubectl rollout status deployment/payment-platform -n staging
        env:
          IMAGE_TAG: ${{ github.sha }}
          ENVIRONMENT: staging

      - name: Run smoke tests
        run: |
          kubectl wait --for=condition=ready pod -l app=payment-platform -n staging --timeout=300s
          ./scripts/smoke-tests.sh staging

      - name: Notify deployment
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Staging deployment completed'
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}

  deploy-production:
    name: Deploy to Production
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event.workflow_run.conclusion == 'success'
    environment: production

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID_PROD }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY_PROD }}
          aws-region: us-east-1

      - name: Set up kubectl
        uses: azure/setup-kubectl@v3

      - name: Update kubeconfig
        run: aws eks update-kubeconfig --name payment-platform-production

      - name: Pre-deployment checks
        run: |
          # Constitutional compliance check
          ./scripts/pre-deployment-checks.sh production

          # Security validation
          ./scripts/security-validation.sh production

          # Database migration dry run
          kubectl create job migration-dry-run --from=cronjob/db-migration --dry-run=client -o yaml | kubectl apply -f -

      - name: Blue-Green deployment
        run: |
          # Deploy to green environment
          envsubst < k8s/production/deployment-green.yaml | kubectl apply -f -
          kubectl rollout status deployment/payment-platform-green -n production

          # Run production smoke tests
          ./scripts/smoke-tests.sh production-green

          # Switch traffic to green
          kubectl patch service payment-platform -n production -p '{"spec":{"selector":{"version":"green"}}}'

          # Wait and validate
          sleep 30
          ./scripts/production-validation.sh

          # Scale down blue environment
          kubectl scale deployment payment-platform-blue --replicas=0 -n production

      - name: Post-deployment validation
        run: |
          # Comprehensive post-deployment tests
          ./scripts/post-deployment-tests.sh production

          # Performance validation
          ./scripts/performance-validation.sh production

          # Security validation
          ./scripts/security-validation.sh production

      - name: Create release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: v${{ github.run_number }}
          release_name: Production Release v${{ github.run_number }}
          body: |
            Production deployment completed successfully.

            Deployed commit: ${{ github.sha }}

            Constitutional compliance: ✅
            Security validation: ✅
            Performance validation: ✅

      - name: Notify production deployment
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Production deployment completed successfully'
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

## Docker Configuration

### Multi-stage Dockerfile
```dockerfile
# docker/Dockerfile
FROM openjdk:21-jdk-slim as builder

WORKDIR /app

# Copy Gradle files
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle .
COPY build.gradle .

# Copy source code
COPY src src

# Build application
RUN ./gradlew bootJar --no-daemon

# Production stage
FROM openjdk:21-jre-slim

# Security: Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install security updates
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    curl \
    jq && \
    rm -rf /var/lib/apt/lists/*

# Application setup
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Health check script
COPY docker/healthcheck.sh /app/healthcheck.sh
RUN chmod +x /app/healthcheck.sh

# Security: Change ownership and switch to non-root user
RUN chown -R appuser:appuser /app
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD /app/healthcheck.sh

# Expose port
EXPOSE 8080

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Health Check Script
```bash
#!/bin/bash
# docker/healthcheck.sh

# Constitutional requirement: Comprehensive health validation
HEALTH_ENDPOINT="http://localhost:8080/actuator/health"
READINESS_ENDPOINT="http://localhost:8080/actuator/health/readiness"

# Check application health
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $HEALTH_ENDPOINT)
if [ "$HEALTH_STATUS" != "200" ]; then
    echo "Health check failed: HTTP $HEALTH_STATUS"
    exit 1
fi

# Check readiness
READINESS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $READINESS_ENDPOINT)
if [ "$READINESS_STATUS" != "200" ]; then
    echo "Readiness check failed: HTTP $READINESS_STATUS"
    exit 1
fi

# Constitutional compliance check
CONSTITUTIONAL_STATUS=$(curl -s $HEALTH_ENDPOINT | jq -r '.components.constitutional.status // "DOWN"')
if [ "$CONSTITUTIONAL_STATUS" != "UP" ]; then
    echo "Constitutional compliance check failed"
    exit 1
fi

echo "Health check passed"
exit 0
```

## Kubernetes Deployment

### Production Deployment Configuration
```yaml
# k8s/production/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-platform
  namespace: production
  labels:
    app: payment-platform
    version: blue
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: payment-platform
      version: blue
  template:
    metadata:
      labels:
        app: payment-platform
        version: blue
    spec:
      serviceAccountName: payment-platform
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
      containers:
      - name: payment-platform
        image: ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        - name: REDIS_URL
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: url
        - name: STRIPE_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: stripe-secret
              key: secret-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
            - ALL
        volumeMounts:
        - name: tmp
          mountPath: /tmp
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: tmp
        emptyDir: {}
      - name: logs
        emptyDir: {}
      nodeSelector:
        kubernetes.io/arch: amd64
      tolerations:
      - key: "payment-workload"
        operator: "Equal"
        value: "true"
        effect: "NoSchedule"
---
apiVersion: v1
kind: Service
metadata:
  name: payment-platform
  namespace: production
spec:
  selector:
    app: payment-platform
  ports:
  - port: 80
    targetPort: 8080
    name: http
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: payment-platform
  namespace: production
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - api.paymentplatform.com
    secretName: payment-platform-tls
  rules:
  - host: api.paymentplatform.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: payment-platform
            port:
              number: 80
```

## Database Migration Pipeline

### Flyway Migration Job
```yaml
# k8s/jobs/db-migration.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: db-migration
  namespace: production
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: flyway
        image: flyway/flyway:latest
        command:
        - flyway
        - -url=${DATABASE_URL}
        - -user=${DATABASE_USER}
        - -password=${DATABASE_PASSWORD}
        - -locations=filesystem:/flyway/sql
        - migrate
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: url
        - name: DATABASE_USER
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: username
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: database-secret
              key: password
        volumeMounts:
        - name: migration-scripts
          mountPath: /flyway/sql
      volumes:
      - name: migration-scripts
        configMap:
          name: db-migration-scripts
```

## Monitoring and Observability

### Prometheus Monitoring Configuration
```yaml
# k8s/monitoring/servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: payment-platform
  namespace: production
spec:
  selector:
    matchLabels:
      app: payment-platform
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
    scrapeTimeout: 10s
---
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: payment-platform-alerts
  namespace: production
spec:
  groups:
  - name: payment-platform
    rules:
    - alert: PaymentPlatformDown
      expr: up{job="payment-platform"} == 0
      for: 1m
      labels:
        severity: critical
      annotations:
        summary: "Payment platform is down"
        description: "Payment platform has been down for more than 1 minute"

    - alert: HighErrorRate
      expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
      for: 2m
      labels:
        severity: warning
      annotations:
        summary: "High error rate detected"
        description: "Error rate is {{ $value }} requests per second"

    - alert: ConstitutionalViolation
      expr: constitutional_compliance_violations_total > 0
      for: 0s
      labels:
        severity: critical
      annotations:
        summary: "Constitutional compliance violation detected"
        description: "Constitutional principle violation: {{ $labels.principle }}"
```

## Deployment Scripts

### Pre-deployment Validation
```bash
#!/bin/bash
# scripts/pre-deployment-checks.sh

set -euo pipefail

ENVIRONMENT=$1

echo "Running pre-deployment checks for $ENVIRONMENT..."

# Constitutional compliance check
echo "Checking constitutional compliance..."
if ! kubectl exec -n $ENVIRONMENT deployment/payment-platform -- \
    curl -s http://localhost:8080/actuator/health/constitutional | jq -e '.status == "UP"'; then
    echo "ERROR: Constitutional compliance check failed"
    exit 1
fi

# Database connectivity
echo "Checking database connectivity..."
if ! kubectl exec -n $ENVIRONMENT deployment/payment-platform -- \
    curl -s http://localhost:8080/actuator/health/db | jq -e '.status == "UP"'; then
    echo "ERROR: Database connectivity check failed"
    exit 1
fi

# External service connectivity
echo "Checking external service connectivity..."
if ! kubectl exec -n $ENVIRONMENT deployment/payment-platform -- \
    curl -s http://localhost:8080/actuator/health/stripe | jq -e '.status == "UP"'; then
    echo "ERROR: Stripe connectivity check failed"
    exit 1
fi

# Resource availability
echo "Checking resource availability..."
NODE_MEMORY=$(kubectl top nodes --no-headers | awk '{sum += $4} END {print sum}')
if [ "$NODE_MEMORY" -gt 80 ]; then
    echo "WARNING: High memory usage on nodes: ${NODE_MEMORY}%"
fi

echo "Pre-deployment checks completed successfully"
```

### Rollback Script
```bash
#!/bin/bash
# scripts/rollback.sh

set -euo pipefail

ENVIRONMENT=$1
PREVIOUS_VERSION=$2

echo "Initiating rollback to version $PREVIOUS_VERSION in $ENVIRONMENT..."

# Constitutional requirement: Audit rollback decision
kubectl create configmap rollback-audit-$(date +%s) \
    --from-literal=environment=$ENVIRONMENT \
    --from-literal=previous-version=$PREVIOUS_VERSION \
    --from-literal=rollback-time=$(date -Iseconds) \
    --from-literal=rollback-reason="Manual rollback initiated" \
    -n $ENVIRONMENT

# Perform rollback
kubectl rollout undo deployment/payment-platform -n $ENVIRONMENT --to-revision=$PREVIOUS_VERSION

# Wait for rollback completion
kubectl rollout status deployment/payment-platform -n $ENVIRONMENT --timeout=300s

# Validate rollback
echo "Validating rollback..."
sleep 30

# Health check
if ! curl -s http://localhost:8080/actuator/health | jq -e '.status == "UP"'; then
    echo "ERROR: Rollback validation failed - health check"
    exit 1
fi

# Constitutional compliance check
if ! curl -s http://localhost:8080/actuator/health/constitutional | jq -e '.status == "UP"'; then
    echo "ERROR: Rollback validation failed - constitutional compliance"
    exit 1
fi

echo "Rollback completed successfully"

# Notify stakeholders
curl -X POST -H 'Content-type: application/json' \
    --data "{\"text\":\"🔄 Rollback completed for $ENVIRONMENT to version $PREVIOUS_VERSION\"}" \
    $SLACK_WEBHOOK_URL
```

---

**Agent Version**: 1.0.0
**Pipeline Type**: GitHub Actions + Kubernetes
**Constitutional Compliance**: Required

Use this agent for designing, implementing, and maintaining CI/CD pipelines with constitutional compliance, automated quality gates, blue-green deployments, and comprehensive monitoring while ensuring secure and reliable software delivery.