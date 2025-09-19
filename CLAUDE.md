# Spring Boot Modulith Payment Platform

A production-ready, security-first subscription management and payment processing platform built with modern Spring Boot Modulith architecture.

## Project Overview

This is a comprehensive **payment platform** built with Spring Boot 3.2.0 and Java 21, implementing a modular monolith architecture pattern using Spring Modulith 1.1.0. The platform provides secure subscription management, payment processing, and comprehensive audit capabilities with GDPR compliance.

## Architecture Summary

### Technology Stack
- **Backend**: Spring Boot 3.2.0 + Java 21 + Spring Modulith 1.1.0
- **Frontend**: React 18 + TypeScript 5.3 + Redux Toolkit + Vite
- **Database**: PostgreSQL with comprehensive indexing strategy
- **Cache**: Redis for session management and caching
- **Payments**: Stripe integration with webhook handling
- **Testing**: TestContainers + Vitest + Playwright + ArchUnit
- **Security**: OAuth2/PKCE + opaque tokens + production-hardened configuration

### Core Modules

The system is organized into **6 main modules** following Spring Modulith patterns:

```
com.platform/
├── auth/           # Authentication & authorization with enhanced security
├── payment/        # Stripe integration & payment processing
├── user/           # User & organization management with multi-tenancy
├── subscription/   # Subscription lifecycle & billing management
├── audit/          # Compliance logging & GDPR compliance
└── shared/         # Common utilities & security configuration
```

### Module Communication
- **Event-Driven**: All inter-module communication via ApplicationEventPublisher
- **Strict Boundaries**: ArchUnit tests enforce module separation
- **API Contracts**: Only `api/` and `events/` packages are cross-module accessible
- **Internal Encapsulation**: `internal/` packages are module-private

## Quick Start

### Prerequisites
- **Java 21** (OpenJDK recommended)
- **Node.js 18+** for frontend development
- **Docker** for TestContainers and local services
- **PostgreSQL** and **Redis** (can be run via Docker)

### Backend Development
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=test'

# Run tests with architecture validation
./gradlew test
./gradlew test --tests "*ArchitectureTest"
./gradlew test --tests "*ModuleBoundaryTest"

# Check code quality
./gradlew checkstyleMain checkstyleTest
```

### Frontend Development
```bash
cd frontend
npm install
npm run dev

# Run comprehensive tests
npm run test
npm run test:e2e
npm run typecheck

# Build with bundle analysis
npm run build:analyze
npm run bundle:analyze
