# 🚀 Payment Platform - Comprehensive Deployment Guide

This guide provides complete instructions for deploying the Spring Boot Modulith Payment Platform with enterprise-grade CI/CD, monitoring, and infrastructure management.

## 📋 Table of Contents

1. [Quick Start](#quick-start)
2. [Prerequisites](#prerequisites)
3. [Infrastructure Setup](#infrastructure-setup)
4. [CI/CD Pipeline](#cicd-pipeline)
5. [Deployment Strategies](#deployment-strategies)
6. [Monitoring & Observability](#monitoring--observability)
7. [Security Configuration](#security-configuration)
8. [Troubleshooting](#troubleshooting)
9. [Maintenance Operations](#maintenance-operations)

## 🚀 Quick Start

### Local Development Setup
```bash
# 1. Setup the complete environment
make setup

# 2. Start development servers
make dev

# 3. Run comprehensive tests
make test-all

# 4. Access the applications
# Frontend: http://localhost:3000
# Backend: http://localhost:8082
# Grafana: http://localhost:3001
```

### Production Deployment
```bash
# 1. Deploy to staging
make deploy-staging

# 2. Deploy to production (with safety checks)
make deploy-production

# 3. Monitor deployment health
make health && make monitor
```

## 📦 Prerequisites

### Required Tools
- **Java 21** (OpenJDK recommended)
- **Node.js 18+** with npm
- **Docker** and Docker Compose
- **kubectl** and Helm (for Kubernetes deployments)
- **Terraform** (for infrastructure provisioning)
- **Git** with proper SSH keys

### Cloud Resources
- **Kubernetes cluster** (EKS, GKE, or AKS)
- **Managed database** (PostgreSQL 15+)
- **Redis cache** (ElastiCache or equivalent)
- **Container registry** (ECR, GCR, or DockerHub)
- **DNS management** for custom domains
- **SSL certificates** (Let's Encrypt or purchased)

## 🏗️ Infrastructure Setup

### 1. Provision Infrastructure with Terraform

```bash
# Navigate to terraform directory
cd terraform

# Initialize Terraform
terraform init

# Plan infrastructure changes
terraform plan -var="environment=staging"

# Apply infrastructure (staging)
terraform apply -var="environment=staging" -auto-approve

# Apply infrastructure (production)
terraform apply -var="environment=production" -auto-approve
```

### 2. Configure Kubernetes Context

```bash
# Configure kubectl for your cluster
aws eks update-kubeconfig --region us-west-2 --name payment-platform-staging
aws eks update-kubeconfig --region us-west-2 --name payment-platform-production

# Verify cluster access
kubectl cluster-info
kubectl get nodes
```

### 3. Deploy Monitoring Stack

```bash
# Deploy monitoring to staging
cd terraform/modules/monitoring
terraform apply -var="environment=staging" -var="cluster_name=payment-platform-staging"

# Deploy monitoring to production
terraform apply -var="environment=production" -var="cluster_name=payment-platform-production"
```

## 🔄 CI/CD Pipeline

### GitHub Actions Workflows

The platform includes multiple automated workflows:

#### 1. Enhanced CI/CD Pipeline (`enhanced-ci-cd.yml`)
- **Triggers**: Push to main/develop, PRs, releases
- **Features**:
  - Multi-stage parallel testing
  - Quality gates enforcement
  - Blue-green deployment
  - Automatic rollback on failure

#### 2. Quality Gates (`quality-gates.yml`)
- **Code Quality Gate**: Checkstyle, ESLint, security scanning
- **Coverage Gate**: 80% minimum test coverage
- **Performance Gate**: <2s response time threshold
- **Security Gate**: Zero critical vulnerabilities
- **Architecture Gate**: Module boundary compliance

#### 3. Deployment Workflows
- **Staging**: Auto-deploy on `develop` branch
- **Production**: Manual trigger on releases
- **Rollback**: Automated rollback procedures

### Setting Up CI/CD

1. **Configure Secrets** in GitHub repository:
   ```bash
   # Required secrets for CI/CD
   SONAR_TOKEN=your_sonar_token
   DOCKER_REGISTRY_TOKEN=your_registry_token
   STAGING_KUBE_CONFIG=base64_encoded_kubeconfig
   PRODUCTION_KUBE_CONFIG=base64_encoded_kubeconfig
   SLACK_WEBHOOK=your_slack_webhook_url
   ```

2. **Configure Branch Protection Rules**:
   - Require status checks to pass
   - Require branches to be up to date
   - Require review from code owners
   - Include administrators in restrictions

## 🎯 Deployment Strategies

### Blue-Green Deployment

#### Staging Environment
```bash
# Deploy to staging with blue-green strategy
make deploy-staging

# Manual verification commands
make health-staging
kubectl get pods -n payment-platform-staging
```

#### Production Environment
```bash
# Production deployment with comprehensive safety checks
make deploy-production

# The script will:
# 1. Create database backup
# 2. Deploy to green environment
# 3. Run comprehensive tests
# 4. Switch traffic gradually
# 5. Monitor for issues
# 6. Cleanup old environment
```

### Rollback Procedures

#### Automatic Rollback
- **Trigger**: Health check failures, error rate spikes
- **Action**: Automatic traffic switch to previous version
- **Notification**: Immediate alerts to team

#### Manual Rollback
```bash
# Emergency rollback to previous version
make rollback

# Rollback to specific version
./scripts/rollback.sh production v1.2.0

# Rollback to specific revision
./scripts/rollback.sh production revision-5
```

## 📊 Monitoring & Observability

### Monitoring Stack Components

#### 1. Prometheus (Metrics Collection)
- **Backend metrics**: JVM, HTTP requests, database connections
- **Business metrics**: Payment success rates, revenue, user activity
- **Infrastructure metrics**: CPU, memory, disk, network

#### 2. Grafana (Visualization)
- **Dashboards**: Overview, Spring Boot metrics, business KPIs
- **Alerts**: Configurable thresholds with Slack integration
- **Access**: https://grafana.payment-platform.com

#### 3. AlertManager (Alert Routing)
- **Critical alerts**: Slack #alerts-critical channel
- **Warning alerts**: Slack #alerts-warning channel
- **Business alerts**: Payment failures, revenue drops

#### 4. Jaeger (Distributed Tracing)
- **Request tracing**: End-to-end request flow
- **Performance analysis**: Latency bottlenecks
- **Error correlation**: Link errors across services

### Key Metrics to Monitor

#### Application Metrics
- **Error Rate**: < 0.1% for critical endpoints
- **Response Time**: 95th percentile < 2 seconds
- **Throughput**: Requests per second
- **Database Connection Pool**: < 80% utilization

#### Business Metrics
- **Payment Success Rate**: > 99.5%
- **Daily Revenue**: Trending and thresholds
- **Active Users**: Daily/monthly active users
- **Chargeback Rate**: < 1%

#### Infrastructure Metrics
- **CPU Utilization**: < 70% average
- **Memory Usage**: < 80% of allocated
- **Disk Usage**: < 80% capacity
- **Network Latency**: < 100ms internal

## 🔒 Security Configuration

### 1. Application Security

#### Backend Security
- **Authentication**: OAuth2 + PKCE with opaque tokens
- **Authorization**: Role-based access control (RBAC)
- **Session Management**: Redis-backed secure sessions
- **API Security**: Rate limiting, input validation

#### Frontend Security
- **CSP Headers**: Strict content security policy
- **HTTPS Only**: All communication encrypted
- **Input Validation**: Client and server-side validation
- **XSS Protection**: React built-in + custom headers

### 2. Infrastructure Security

#### Network Security
- **VPC**: Private subnets for application tiers
- **Security Groups**: Minimal required ports
- **WAF**: Web application firewall for public endpoints
- **DDoS Protection**: CloudFlare or equivalent

#### Data Security
- **Encryption at Rest**: All databases and storage
- **Encryption in Transit**: TLS 1.3 for all connections
- **Key Management**: AWS KMS or equivalent
- **Backup Encryption**: GPG encrypted database backups

### 3. Compliance & Auditing

#### PCI DSS Compliance
- **Card Data**: Never stored, processed via Stripe
- **Tokenization**: Secure token exchange
- **Audit Logging**: All payment transactions logged
- **Access Control**: Principle of least privilege

#### GDPR Compliance
- **Data Retention**: Automated cleanup policies
- **Data Subject Rights**: Export and deletion capabilities
- **Privacy by Design**: Minimal data collection
- **Audit Trail**: Complete activity logging

## 🔧 Troubleshooting

### Common Issues and Solutions

#### Deployment Issues

**Issue**: Deployment stuck in "Progressing" state
```bash
# Check deployment status
kubectl rollout status deployment/payment-platform-backend-production -n payment-platform-production

# Check pod logs
kubectl logs -l app=payment-platform-backend -n payment-platform-production --tail=100

# Force rollback if needed
make rollback
```

**Issue**: Database connection failures
```bash
# Check database connectivity
kubectl exec -it deployment/payment-platform-backend-production -n payment-platform-production -- \
  curl -f http://localhost:8080/actuator/health/db

# Check database pod status (if using in-cluster database)
kubectl get pods -l app=postgres -n payment-platform-production
```

#### Performance Issues

**Issue**: High response times
```bash
# Check application metrics
kubectl port-forward service/prometheus 9090:9090
# Open http://localhost:9090 and query: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Check resource usage
kubectl top pods -n payment-platform-production

# Scale up if needed
kubectl scale deployment payment-platform-backend-production --replicas=5 -n payment-platform-production
```

#### Quality Gate Failures

**Issue**: Test coverage below threshold
```bash
# Generate detailed coverage report
make coverage-report

# View coverage reports
open build/reports/jacoco/test/html/index.html  # Backend
open frontend/coverage/index.html               # Frontend
```

**Issue**: Security vulnerabilities detected
```bash
# Run security audit
make security-audit

# Update dependencies
make update-deps

# Re-run security scan
make security-scan-local
```

### Health Check Commands

```bash
# Comprehensive health check
make troubleshoot

# Check all service endpoints
make health

# Monitor real-time metrics
make monitor

# View application logs
kubectl logs -f deployment/payment-platform-backend-production -n payment-platform-production
```

## 🔄 Maintenance Operations

### Database Operations

#### Backup Operations
```bash
# Create manual backup
./scripts/backup-database.sh production full

# Restore from backup
./scripts/restore-database.sh production /path/to/backup.sql.gz

# Schedule automated backups (runs via cron)
# 0 2 * * * /path/to/scripts/backup-database.sh production full
```

#### Database Migrations
```bash
# Run migrations in staging
cd backend
./gradlew flywayMigrate -Pflyway-staging

# Validate migrations
./gradlew flywayValidate -Pflyway-staging

# Run migrations in production (with backup)
./scripts/backup-database.sh production pre-migration
./gradlew flywayMigrate -Pflyway-prod
```

### Scaling Operations

#### Manual Scaling
```bash
# Scale backend replicas
kubectl scale deployment payment-platform-backend-production --replicas=10 -n payment-platform-production

# Scale frontend replicas
kubectl scale deployment payment-platform-frontend-production --replicas=5 -n payment-platform-production

# Verify scaling
kubectl get pods -n payment-platform-production
```

#### Auto-scaling Configuration
```yaml
# HPA is already configured in production.yml
# Scales based on CPU (70%) and Memory (80%) utilization
# Min replicas: 3, Max replicas: 10
```

### Certificate Management

#### SSL Certificate Renewal
```bash
# Check certificate expiry
openssl s_client -connect payment-platform.com:443 -servername payment-platform.com 2>/dev/null | \
  openssl x509 -noout -dates

# Renew Let's Encrypt certificates (handled automatically by cert-manager)
kubectl get certificates -n payment-platform-production
```

### Monitoring Maintenance

#### Metric Retention
```bash
# Clean up old Prometheus data (if needed)
kubectl exec -it prometheus-server-0 -n monitoring -- \
  rm -rf /prometheus/data/01234567890123456789  # old data directory
```

#### Dashboard Updates
```bash
# Update Grafana dashboards
kubectl create configmap grafana-dashboards \
  --from-file=config/grafana/dashboards/ \
  --dry-run=client -o yaml | kubectl apply -f - -n monitoring
```

## 📞 Support and Escalation

### Monitoring and Alerting Channels
- **Slack #alerts-critical**: Critical system alerts
- **Slack #alerts-warning**: Warning level alerts
- **Slack #deployments**: Deployment notifications
- **Email**: alerts@payment-platform.com

### Runbook References
- **High Error Rate**: https://docs.payment-platform.com/runbooks/high-error-rate
- **Database Issues**: https://docs.payment-platform.com/runbooks/database-connections
- **Payment Failures**: https://docs.payment-platform.com/runbooks/payment-failures
- **Performance Issues**: https://docs.payment-platform.com/runbooks/high-response-time

### Emergency Contacts
- **On-Call Engineer**: Slack @oncall or phone +1-XXX-XXX-XXXX
- **Platform Team Lead**: engineer@payment-platform.com
- **DevOps Team**: devops@payment-platform.com

---

## 📝 Additional Resources

- **API Documentation**: https://api.payment-platform.com/swagger-ui.html
- **Architecture Documentation**: `/docs/architecture/`
- **Security Guidelines**: `/docs/security/`
- **Development Setup**: `/backend/CLAUDE.md` and `/frontend/CLAUDE.md`

---

*This deployment guide is maintained by the Payment Platform DevOps team. For updates or questions, please contact devops@payment-platform.com.*