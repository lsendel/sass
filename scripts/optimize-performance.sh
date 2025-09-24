#!/bin/bash

# SASS Performance Optimization Script
set -e

echo "🚀 SASS Performance Optimization"
echo "================================"

# Backend optimizations
echo "⚙️ Applying backend optimizations..."

# 1. Add JVM optimization flags
cat > backend/gradle.properties << EOF
# JVM Optimization
org.gradle.jvmargs=-Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication
org.gradle.parallel=true
org.gradle.caching=true

# Spring Boot optimizations
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true

# Connection pool optimization
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000
EOF

# 2. Frontend optimizations
echo "📱 Applying frontend optimizations..."

# Add Vite optimization config
cat > frontend/vite.config.optimization.ts << EOF
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          ui: ['@headlessui/react', '@heroicons/react'],
          utils: ['axios', 'date-fns', 'clsx']
        }
      }
    },
    chunkSizeWarningLimit: 1000,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    }
  },
  server: {
    hmr: {
      overlay: false
    }
  }
})
EOF

# 3. Database optimization
echo "🗄️ Creating database optimization migration..."
mkdir -p backend/src/main/resources/db/migration
cat > backend/src/main/resources/db/migration/V999__performance_indexes.sql << EOF
-- Performance optimization indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_organization_id ON users(organization_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_created_at ON payments(created_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

-- Analyze tables for better query planning
ANALYZE users;
ANALYZE payments;
ANALYZE audit_logs;
EOF

# 4. Add caching configuration
echo "💾 Adding caching configuration..."
cat > backend/src/main/resources/cache-config.yml << EOF
spring:
  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10 minutes
      cache-null-values: false
  data:
    redis:
      repositories:
        enabled: true
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
EOF

# 5. Add monitoring configuration
echo "📊 Adding performance monitoring..."
cat > backend/src/main/resources/monitoring.yml << EOF
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
EOF

echo "✅ Performance optimizations applied!"
echo ""
echo "📋 Next steps:"
echo "1. Review and merge the generated configuration files"
echo "2. Test the optimizations in development"
echo "3. Monitor performance metrics after deployment"
echo "4. Adjust settings based on actual usage patterns"
