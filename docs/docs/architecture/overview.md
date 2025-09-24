# SASS Architecture Overview

## System Context

```mermaid
graph TB
    subgraph "Client Applications"
        U1[Web Browser]
        U2[Mobile App]
        U3[Third-party Systems]
    end

    subgraph "SASS Platform"
        subgraph "Frontend Layer"
            A[React/TypeScript Frontend]
        end

        subgraph "Backend Services"
            B[API Gateway]
            C[Auth Service]
            D[Payment Service]
            E[Subscription Service]
            F[User Management]
            G[Audit Service]
        end

        subgraph "Data Layer"
            H[(PostgreSQL)]
            I[(Redis Cache)]
            J[(Stripe API)]
        end

        subgraph "Constitutional Tools"
            K[Python Agents]
            L[AI Enforcement]
        end
    end

    subgraph "External Services"
            M[OAuth Providers]
            N[Email Service]
            O[Monitoring]
        end

    U1 --> A
    U2 --> A
    U3 --> B
    A --> B
    B --> C
    B --> D
    B --> E
    B --> F
    B --> G
    C --> H
    D --> H
    E --> H
    F --> H
    G --> H
    D --> I
    D --> J
    K --> L
    C --> M
    G --> N
    B --> O
    D --> O
```

## Container Diagram

```mermaid
graph TB
    subgraph "SASS Deployment"
        subgraph "Frontend Container"
            A[Vite React App<br/>Port 3000]
        end

        subgraph "Backend Container"
            B[Spring Boot App<br/>Port 8082]
            B1[OAuth2 Handler]
            B2[Payment Processor]
            B3[Subscription Manager]
            B4[Security Layer]
        end

        subgraph "Database Container"
            C[PostgreSQL<br/>Port 5432]
        end

        subgraph "Cache Container"
            D[Redis<br/>Port 6379]
        end

        subgraph "Constitutional Tools Container"
            E[Python Agents<br/>Various Ports]
        end
    end

    A <--> B
    B <--> C
    B <--> D
    E <--> B
```

## Component Diagram (Backend)

```mermaid
graph TB
    subgraph "Spring Boot Application"
        subgraph "Auth Module"
            A1[OAuth2 Configuration]
            A2[User Authentication]
            A3[Token Management]
            A4[Security Filters]
        end

        subgraph "Payment Module"
            B1[Stripe Service]
            B2[Payment Processor]
            B3[Webhook Handler]
            B4[Transaction Manager]
        end

        subgraph "Subscription Module"
            C1[Plan Manager]
            C2[Subscription Service]
            C3[Billing Calculator]
            C4[Renewal Processor]
        end

        subgraph "User Module"
            D1[User Service]
            D2[Organization Manager]
            D3[Profile Management]
            D4[Permission Manager]
        end

        subgraph "Shared Components"
            E1[Security Utilities]
            E2[Event Publisher]
            E3[Configuration]
            E4[Common DTOs]
        end

        subgraph "Audit Module"
            F1[Audit Service]
            F2[Event Logger]
            F3[Compliance Checker]
        end
    end

    A2 --> E2
    B2 --> E2
    C2 --> E2
    D1 --> E2
    F2 --> E2
    A4 --> E1
    A2 --> D1
    B2 --> D1
    C2 --> D1
    C2 --> B2
    F1 --> A2
    F1 --> B2
    F1 --> C2
    F1 --> D1
```

## Deployment Architecture

```mermaid
graph TB
    subgraph "Production Environment"
        subgraph "Load Balancer Layer"
            LB[Load Balancer]
        end

        subgraph "Application Layer"
            A1[Backend Instance 1]
            A2[Backend Instance 2]
            A3[Backend Instance 3]
        end

        subgraph "Frontend Layer"
            F1[Frontend Instance 1]
            F2[Frontend Instance 2]
        end

        subgraph "Database Layer"
            DB[(PostgreSQL Cluster)]
        end

        subgraph "Cache Layer"
            RC[(Redis Cluster)]
        end

        subgraph "Monitoring Layer"
            M1[Prometheus]
            M2[Grafana]
            M3[ELK Stack]
        end

        subgraph "Security Layer"
            W[Web Application Firewall]
            KMS[Key Management Service]
        end
    end

    Client --> LB
    LB --> A1
    LB --> A2
    LB --> A3
    LB --> F1
    LB --> F2
    A1 --> DB
    A2 --> DB
    A3 --> DB
    A1 --> RC
    A2 --> RC
    A3 --> RC
    A1 --> M1
    A2 --> M1
    A3 --> M1
    M1 --> M2
    A1 --> M3
    A2 --> M3
    A3 --> M3
    W --> LB
    KMS --> A1
    KMS --> A2
    KMS --> A3
```

## Data Flow for Payment Processing

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant B as Backend
    participant S as Stripe
    participant D as Database
    participant A as Audit

    U->>F: Submit payment request
    F->>B: POST /api/v1/payments
    B->>D: Validate user/subscription
    D-->>B: User & subscription data
    B->>S: Create payment intent
    S-->>B: Payment intent + client secret
    B->>D: Store payment intent
    B-->>F: Return client secret
    F->>S: Confirm payment (client-side)
    S->>B: webhook payment.success
    B->>A: Log payment event
    B->>D: Update payment status
    A-->>B: Audit confirmation
    B-->>F: Payment confirmed
    F-->>U: Show success
```

## Development Architecture

The SASS project implements a unique dual-stack architecture:

### Application Stack (Java/TypeScript)
- **Backend**: Spring Boot application with modular architecture
  - Built with Java 21 and Spring Boot 3.5.5
  - Uses Spring Modulith for modular design
  - Modules: auth, payment, subscription, user, audit, shared
  - Repository pattern with JPA/Hibernate
  - RESTful APIs with OpenAPI documentation

- **Frontend**: React application with TypeScript
  - Built with Vite for fast development
  - Uses Tailwind CSS for styling
  - State management with Redux Toolkit
  - API communication via RTK Query
  - Component testing with React Testing Library

### Constitutional Tools Stack (Python)
- **Purpose**: AI-powered development assistance and enforcement
- **Components**: Specialized Python agents for development practices
- **Features**:
  - Constitutional enforcement agent
  - TDD compliance agent
  - Task coordination agent
  - Architecture validation
  - Security compliance validation
  - Multi-agent workflow coordination

## Security Architecture

### Authentication Flow
1. OAuth2 providers (Google, GitHub, etc.)
2. Custom OAuth2UserService for user mapping
3. OpaqueTokenAuthenticationFilter for token validation
4. TenantContext for multi-tenant isolation
5. SecurityConfig for access control configuration

### Data Protection
- Encryption at rest for sensitive data
- TLS 1.3 for data in transit
- PCI DSS compliance for payment processing
- GDPR compliance for data handling
- Audit logging for all operations

## Event-Driven Architecture

The system uses Spring Modulith's event-driven capabilities:
- Domain events published by modules
- Event listeners for cross-cutting concerns
- Audit logging triggered by domain events
- Payment processing initiated by subscription events

## API Design Principles

### RESTful Design
- Resource-based URLs
- Standard HTTP verbs
- Proper status codes
- HATEOAS where appropriate
- Versioning via URL path

### Error Handling
- Consistent error response format
- Standardized error codes
- Descriptive error messages
- Proper HTTP status codes
- Audit trail for errors

## Deployment Architecture

### Local Development
- Docker Compose for local environment
- Single container per service
- Local PostgreSQL and Redis
- Hot-reload for development

### Production
- Kubernetes deployment
- Multiple replicas for high availability
- Separate namespaces for environments
- Ingress for external access
- Persistent volumes for data
- Monitoring and logging stack

## Quality Assurance Architecture

### Testing Strategy
- Unit tests for business logic (JUnit 5, Vitest)
- Integration tests with Testcontainers
- Contract tests for API consistency
- E2E tests with Playwright
- Security tests for vulnerabilities

### Code Quality
- SonarQube for static analysis
- Checkstyle for code formatting
- Pre-commit hooks for quality gates
- Coverage requirements (>85%)
- Multi-agent constitutional enforcement