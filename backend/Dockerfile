# Multi-stage Docker build for Spring Boot application
# Optimized for production deployment with minimal image size and security

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src/ src/

# Make gradlew executable
RUN chmod +x gradlew

# Build application (skip tests for faster builds - run tests in CI)
RUN ./gradlew clean bootJar -x test --no-daemon

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Install additional packages needed for monitoring/debugging
RUN apk add --no-cache \
    curl \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Set working directory
WORKDIR /app

# Copy application jar from build stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership of the app directory to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables with defaults
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+UseContainerSupport"

# Expose port
EXPOSE 8080

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]