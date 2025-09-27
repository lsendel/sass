---
sidebar_position: 5
title: API Reference
---

# API Reference

Comprehensive API documentation for the Spring Boot Modulith Payment Platform.

## Backend API Documentation

### Java API Documentation
The backend API is built with Spring Boot 3.2.0 and Java 21, following Spring Modulith architecture patterns.

- **JavaDoc API Reference**: generated to `docs/backend-api-javadoc/index.html` during the build step
- **Module Structure**:
  - `com.platform.auth` - OAuth2/PKCE authentication and session management
  - `com.platform.payment` - Stripe integration and payment processing
  - `com.platform.user` - User and organization management
  - `com.platform.subscription` - Subscription lifecycle and billing
  - `com.platform.audit` - GDPR-compliant audit logging
  - `com.platform.shared` - Common utilities and configurations

### REST API Endpoints
The backend exposes RESTful APIs documented with OpenAPI/Swagger:

#### Core API Modules
- **[Authentication API](./api/authentication.md)** - Enhanced OAuth2/PKCE and password authentication with advanced security features
- **Payment Processing** (`/api/payments/*`) - Stripe integration and payment processing
- **User Management** (`/api/users/*`) - User and organization lifecycle management
- **Organization Management** (`/api/organizations/*`) - Multi-tenant organization operations
- **Subscription Management** (`/api/subscriptions/*`) - Subscription lifecycle and billing

#### Analytics & Reporting
- **[Analytics API](./api/analytics.md)** - Comprehensive analytics for users, payments, and security monitoring
- **Security Analytics** - Advanced threat detection and user behavior analysis
- **Business Intelligence** - Revenue analysis, cohort studies, and growth metrics
- **Compliance Reporting** - GDPR, audit trails, and data access patterns

## Frontend API Documentation

### TypeScript/React Documentation
The frontend is built with React 18, TypeScript 5.3, and Redux Toolkit.

- **TypeDoc API Reference**: generated to `docs/frontend-api/index.md` during the build step
- **Component Library**:
  - Authentication components
  - Payment UI components
  - Dashboard layouts
  - Reusable UI components
  - Custom hooks and utilities

### Key Frontend Modules
- **Redux Store** - State management with RTK Query
- **API Integration** - Type-safe API client implementations
- **Components** - React component library
- **Hooks** - Custom React hooks for common functionality
- **Utils** - Utility functions and helpers

## Integration Points

### Authentication Flow
Both frontend and backend work together to implement:
- OAuth2/PKCE authentication
- Secure session management
- Multi-factor authentication support
- Organization-based access control

### Payment Processing
End-to-end payment flow integration:
- Stripe Elements in frontend
- Payment Intent creation in backend
- Webhook handling for payment events
- PCI-compliant implementation

## Development Tools

### Backend Development
```bash
# Generate backend documentation
cd backend
./gradlew javadoc

# View documentation
open ../docs/docs/backend-api-javadoc/index.html
```

### Frontend Development
```bash
# Generate frontend documentation
cd frontend
npm run docs:build

# View documentation
open ../docs/docs/frontend-api/README.md
```

## API Versioning

The platform follows semantic versioning:
- Current Version: **1.0.0**
- API Version Header: `X-API-Version`
- Backward compatibility maintained within major versions

## Security Considerations

All API interactions require:
- **Authentication**: Opaque tokens (no JWT)
- **HTTPS**: Enforced in production
- **CSRF Protection**: Token-based protection
- **Rate Limiting**: Applied to all endpoints
- **Input Validation**: Comprehensive validation on all inputs

## Support & Resources

- Architecture overview (`docs/architecture/overview`)
- Backend module guides (`docs/backend/`)
- Frontend guides (`docs/frontend/`)
- Testing quick start (`docs/tutorial-basics/`)
