# Architecture Overview

The Spring Boot Modulith Payment Platform is built with a modular monolith architecture pattern, providing the benefits of microservices while maintaining the simplicity of a monolithic deployment.

## Core Modules

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

## Technology Stack

### Backend
- **Spring Boot 3.2.0** + **Java 21** + **Spring Modulith 1.1.0**
- **PostgreSQL** with comprehensive indexing strategy
- **Redis** for session management and caching
- **Stripe** integration with webhook handling

### Frontend
- **React 18** + **TypeScript 5.3** + **Redux Toolkit** + **Vite**
- **Tailwind CSS** for styling
- **React Router** for navigation
- **RTK Query** for API integration

### Testing
- **TestContainers** for integration testing
- **Vitest** for frontend unit testing
- **Playwright** for end-to-end testing
- **ArchUnit** for architecture compliance

## Module Communication

- **Event-Driven**: All inter-module communication via ApplicationEventPublisher
- **Strict Boundaries**: ArchUnit tests enforce module separation
- **API Contracts**: Only `api/` and `events/` packages are cross-module accessible
- **Internal Encapsulation**: `internal/` packages are module-private

## Security Features

- **Enhanced Password Policies**: 12+ character requirements with complexity validation
- **Account Protection**: Exponential backoff lockout mechanisms
- **Production Security Config**: Strict CORS, CSP headers, HSTS with preload
- **Session Security**: Redis-backed opaque tokens (no JWT for security)
- **Audit Trail**: Comprehensive logging with GDPR compliance
- **PCI Compliance**: Secure payment processing with Stripe integration