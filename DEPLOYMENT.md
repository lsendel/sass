# Production Deployment Guide

This guide covers deploying the Spring Boot Modulith Payment Platform to production environments.

## Prerequisites

### System Requirements
- **OS**: Linux server (Ubuntu 20.04+ recommended)
- **Memory**: Minimum 4GB RAM (8GB recommended for production)
- **Storage**: 20GB minimum (50GB recommended for logs and backups)
- **CPU**: 2+ cores (4+ cores recommended for production)

### Software Dependencies
- **Docker**: 20.10+ and Docker Compose v2
- **Kubernetes**: v1.25+ (for K8s deployment)
- **SSL Certificates**: Valid TLS certificates for HTTPS
- **Domain**: Configured domain name with DNS

### Required API Keys and Credentials
- **Stripe**: Live API keys (publishable and secret)
- **OAuth Providers**: Google, GitHub, Microsoft OAuth2 applications
- **Email Service**: SMTP credentials or service API keys
- **Database**: PostgreSQL 15+ connection details
- **Redis**: Redis 6+ connection details

## Quick Start

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd payment-platform
   ```

2. **Configure environment:**
   ```bash
   cp .env.production.example .env.production
   # Edit .env.production with your actual values
   ```

3. **Deploy:**
   ```bash
   ./deploy.sh -e production
   ```

## Environment Configuration

### Required Environment Variables

Create `.env.production` file with the following variables:

```bash
# Database
POSTGRES_PASSWORD=your_secure_db_password
REDIS_PASSWORD=your_secure_redis_password

# Stripe
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
VITE_STRIPE_PUBLISHABLE_KEY=pk_live_...

# OAuth
OAUTH_GOOGLE_CLIENT_ID=...
OAUTH_GOOGLE_CLIENT_SECRET=...
# ... other OAuth providers
```

### SSL Configuration

1. **Obtain SSL certificates:**
   ```bash
   # Using Let's Encrypt (recommended)
   certbot certonly --standalone -d yourdomain.com
   ```

2. **Configure nginx:**
   - Update `frontend/default.conf` with your domain
   - Place certificates in `nginx/ssl/` directory

## Deployment Options

### Option 1: Docker Compose (Recommended)

```bash
# Development
docker-compose up -d

# Production
docker-compose -f docker-compose.prod.yml up -d
```

### Option 2: Kubernetes

```bash
# Apply configurations
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/backend.yaml
kubectl apply -f k8s/frontend.yaml
```

### Option 3: Automated Script

```bash
# Full deployment with tests
./deploy.sh -e production

# Skip tests (faster)
./deploy.sh -e production -s

# Force recreate containers
./deploy.sh -e production -f
```

## Monitoring

### Prometheus Metrics

Access Prometheus at `http://your-domain:9090`

Key metrics to monitor:
- `http_requests_total` - HTTP request count
- `jvm_memory_used_bytes` - JVM memory usage
- `payment_attempts_total` - Payment attempts
- `stripe_webhook_failures_total` - Stripe webhook failures

### Grafana Dashboards

Access Grafana at `http://your-domain:3001`
- Username: `admin`
- Password: Set in `GRAFANA_ADMIN_PASSWORD`

### Health Checks

- Backend: `http://your-domain:8080/actuator/health`
- Frontend: `http://your-domain/health`

## Security Considerations

### Production Checklist

- [ ] Use strong passwords for all services
- [ ] Enable SSL/TLS for all connections
- [ ] Configure firewall rules
- [ ] Set up regular backups
- [ ] Enable audit logging
- [ ] Configure rate limiting
- [ ] Use production OAuth credentials
- [ ] Enable HTTPS redirect
- [ ] Configure security headers
- [ ] Set up monitoring alerts

### Firewall Configuration

```bash
# Allow HTTP/HTTPS
ufw allow 80/tcp
ufw allow 443/tcp

# Allow SSH (if needed)
ufw allow 22/tcp

# Block direct access to services
ufw deny 5432/tcp  # PostgreSQL
ufw deny 6379/tcp  # Redis
ufw deny 8080/tcp  # Backend (behind nginx)
```

## Backup and Recovery

### Database Backup

```bash
# Manual backup
docker-compose -f docker-compose.prod.yml exec postgres pg_dump -U paymentuser paymentplatform > backup.sql

# Automated backup (add to cron)
0 2 * * * /path/to/backup-script.sh
```

### Full System Backup

```bash
# Backup volumes
docker run --rm -v payment-platform_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz /data
docker run --rm -v payment-platform_redis_data:/data -v $(pwd):/backup alpine tar czf /backup/redis-backup.tar.gz /data
```

### Restore from Backup

```bash
# Stop services
docker-compose -f docker-compose.prod.yml down

# Restore database
docker-compose -f docker-compose.prod.yml up -d postgres
docker-compose -f docker-compose.prod.yml exec -T postgres psql -U paymentuser paymentplatform < backup.sql

# Start all services
docker-compose -f docker-compose.prod.yml up -d
```

## Troubleshooting

### Common Issues

1. **Service won't start:**
   ```bash
   # Check logs
   docker-compose -f docker-compose.prod.yml logs [service-name]

   # Check service status
   docker-compose -f docker-compose.prod.yml ps
   ```

2. **Database connection failed:**
   ```bash
   # Check PostgreSQL logs
   docker-compose -f docker-compose.prod.yml logs postgres

   # Test connection
   docker-compose -f docker-compose.prod.yml exec postgres psql -U paymentuser paymentplatform
   ```

3. **High memory usage:**
   ```bash
   # Check memory usage
   docker stats

   # Adjust JVM settings in docker-compose.prod.yml
   JAVA_OPTS: "-Xmx512m -Xms256m"
   ```

### Performance Tuning

1. **Database optimization:**
   ```sql
   -- Monitor slow queries
   SELECT query, mean_time, calls
   FROM pg_stat_statements
   ORDER BY mean_time DESC LIMIT 10;
   ```

2. **Application optimization:**
   - Adjust JVM heap size based on usage
   - Enable connection pooling
   - Configure Redis as cache
   - Use CDN for static assets

## Scaling

### Horizontal Scaling

```bash
# Scale backend services
docker-compose -f docker-compose.prod.yml up -d --scale backend=3

# Use load balancer
# Configure nginx upstream for multiple backend instances
```

### Vertical Scaling

```yaml
# Increase resource limits in docker-compose.prod.yml
backend:
  deploy:
    resources:
      limits:
        memory: 2G
        cpus: '2.0'
```

## Maintenance

### Rolling Updates

```bash
# Build new images
docker-compose -f docker-compose.prod.yml build

# Rolling update
docker-compose -f docker-compose.prod.yml up -d --no-deps backend
docker-compose -f docker-compose.prod.yml up -d --no-deps frontend
```

### Health Monitoring

Set up monitoring alerts for:
- Service availability
- Database connectivity
- Memory/CPU usage
- Payment failure rates
- Stripe webhook failures

### Log Management

```bash
# Rotate logs
docker-compose -f docker-compose.prod.yml logs --tail=1000 > logs/app-$(date +%Y%m%d).log

# Clean old logs
find logs/ -name "*.log" -mtime +30 -delete
```

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review application logs
3. Check monitoring dashboards
4. Contact support team

## References

- [Spring Boot Production Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [Docker Production Deployment](https://docs.docker.com/compose/production/)
- [Nginx Configuration](https://nginx.org/en/docs/)
- [PostgreSQL Tuning](https://www.postgresql.org/docs/current/runtime-config.html)
- [Stripe Production Checklist](https://stripe.com/docs/development/checklist)
