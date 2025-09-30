# 🚀 Payment Platform - Spring Boot Modulith

[![CI/CD Status](https://github.com/payment-platform/sass/workflows/Enhanced%20CI/CD%20Pipeline/badge.svg)](https://github.com/payment-platform/sass/actions)
[![Constitutional Compliance](https://github.com/payment-platform/sass/workflows/Constitutional%20Compliance%20Check/badge.svg)](https://github.com/payment-platform/sass/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=payment-platform_sass&metric=alert_status)](https://sonarcloud.io/dashboard?id=payment-platform_sass)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=payment-platform_sass&metric=coverage)](https://sonarcloud.io/dashboard?id=payment-platform_sass)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=payment-platform_sass&metric=security_rating)](https://sonarcloud.io/dashboard?id=payment-platform_sass)

A comprehensive, enterprise-grade payment platform built with **Spring Boot 3.5** and **Spring Modulith** architecture, featuring modern React frontend, complete CI/CD pipeline, and production-ready infrastructure.

## 🏗️ Architecture Overview

### Backend (Spring Boot Modulith)
- **Java 21** with modern language features
- **Spring Boot 3.5.6** with Spring Modulith 1.4.3
- **Modular Monolith** with strict module boundaries
- **Event-driven architecture** for inter-module communication
- **PostgreSQL 15** for data persistence
- **Redis 7** for session management and caching
- **Stripe API** integration for payment processing
- **SpringDoc OpenAPI 2.8.13** for API documentation

### Frontend (React TypeScript)
- **React 19.1.1** with modern hooks and functional components
- **TypeScript 5.7.2** with strict type checking
- **Redux Toolkit 2.3.0** with RTK Query for state management
- **Vite 7.1.7** for fast development and optimized builds
- **TailwindCSS 4.1.13** with custom design system
- **Playwright 1.55.1** for comprehensive E2E testing

### Infrastructure & DevOps
- **Kubernetes** deployment with Helm charts
- **Docker** containerization with multi-stage builds
- **Terraform** infrastructure as code
- **GitHub Actions** CI/CD with quality gates
- **Prometheus & Grafana** for monitoring
- **ELK Stack** for centralized logging

## 🏗️ Architecture

This project implements a **dual-stack architecture**:

- **Application Stack**: Java Spring Boot backend + React/TypeScript frontend
- **Constitutional Tools**: Python-based AI agents for development enforcement

For detailed architecture information with diagrams, see [Architecture Overview](./docs/docs/architecture/overview.md).

## 🛠️ Quick Start

### Prerequisites
- Java 21 (OpenJDK)
- Node.js 18+
- Python 3.9+
- Docker and Docker Compose
- Git

### Setup
```bash
# Clone the repository
git clone https://github.com/your-username/sass.git
cd sass

# Install all dependencies
make setup

# Start development environment
make dev
```

**Access the application:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8082
- API Documentation: http://localhost:8082/swagger-ui.html
- Health Check: http://localhost:8082/actuator/health

### Demo
```bash
# Run complete demo with authentication
make demo
```

## 🧪 Development

### Testing
```bash
# Run all tests (Java + TypeScript + Python)
make test

# Run specific test suites
make test-backend    # Java application tests
make test-frontend   # TypeScript/React tests
make test-python     # Python constitutional tools
```

### Code Quality
```bash
# Run all quality checks
make lint
make format

# Run security scans
make security-check
```

### Development Commands
```bash
# Full setup (install dependencies, run pre-commit hooks)
make setup

# Start development environment (all services)
make dev

# Run in production-like environment
make prod

# Run all checks before committing
make pre-commit

# Generate documentation
make docs
```

## 📁 Project Structure

```
sass/
├── backend/                    # Java Spring Boot application
│   ├── auth/                   # Authentication & authorization
│   ├── payment/                # Payment processing module
│   ├── subscription/           # Subscription management
│   ├── user/                   # User management
│   ├── audit/                  # Audit logging
│   └── shared/                 # Shared utilities & security
├── frontend/                   # React/TypeScript frontend
│   ├── src/
│   │   ├── components/         # Reusable UI components
│   │   ├── pages/              # Application pages
│   │   ├── store/              # Redux state management
│   │   └── api/                # API client and services
├── tools/                      # Python constitutional tools
│   ├── agents/                 # AI enforcement agents
│   └── workflows/              # Multi-agent coordination
├── docs/                       # Docusaurus documentation site
├── specs/                      # Project specifications
├── tests/                      # Test organization
├── k8s/                        # Kubernetes deployment
├── monitoring/                 # Observability configuration
├── .claude/                    # Claude-specific agent configs
└── Makefile                    # Primary development commands
```

## ✨ Key Features

### Application Features
- 🔐 **Secure Authentication**: OAuth2 with multiple providers + session management
- 💳 **Payment Processing**: Stripe integration with PCI DSS compliance
- 👥 **Multi-Tenant**: Organization-based user isolation
- 📊 **Audit Logging**: Comprehensive activity tracking for compliance
- 🔒 **Security-First**: Built with OWASP Top 10 protection
- 📱 **Modern UI**: Responsive React interface with Tailwind CSS

### Development Features
- 🤖 **AI Development Assistants**: Automated code review and best practice enforcement
- 🧪 **TDD Compliance**: Automated verification of test-driven development
- 🏗️ **Modular Architecture**: Clear boundaries with Spring Modulith
- 📈 **Quality Gates**: 85%+ test coverage requirement
- 🔍 **Security Validation**: Continuous security compliance checking
- 📚 **Automated Documentation**: API docs via OpenAPI/Swagger

## 🛠️ Technology Stack

### Backend (Java)
- Java 21 + Spring Boot 3.5.5
- Spring Security + OAuth2
- Spring Modulith for modular architecture
- Spring Data JPA with PostgreSQL
- Spring Cache with Redis
- Stripe API for payments
- JUnit 5 + Testcontainers for testing

### Frontend (TypeScript)
- React 19 + TypeScript
- Vite for fast builds
- Tailwind CSS for styling
- Redux Toolkit + RTK Query for state management
- Playwright for E2E testing
- Vitest for unit testing

### Constitutional Tools (Python)
- Python 3.9+ with specialized AI agents
- pytest for testing
- Development workflow automation
- Code quality and compliance checking

### Infrastructure
- Docker & Docker Compose for containerization
- Kubernetes for orchestration
- PostgreSQL for primary data storage
- Redis for caching and sessions
- Prometheus & Grafana for monitoring

## 🤝 Contributing

We welcome contributions! Please read our [Contributing Guide](./CONTRIBUTING.md) for details on our code of conduct and development process.

### Getting Started
1. Read the [Architecture Overview](./docs/docs/architecture/overview.md)
2. Follow our [Security Guidelines](./SECURITY.md)
3. Understand our [Glossary of Terms](./GLOSSARY.md)
4. Follow the [Constitutional Development Principles](./.claude/context/project-constitution.md)

### Development Workflow
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes following TDD practices
4. Update documentation as needed
5. Run `make pre-commit` to ensure all checks pass
6. Submit a pull request

## 🚀 Deployment

### Local Development
```bash
make dev  # Start all services locally
```

### Production
```bash
# Build all components
make build-all

# Deploy to Kubernetes
kubectl apply -f k8s/
```

### Docker
```bash
# Development environment
docker-compose up -d

# Production environment
docker-compose -f docker-compose.prod.yml up -d
```

For detailed deployment instructions, see [Deployment Guide](./DEPLOYMENT.md).

## 📚 Documentation

### Getting Started
- [Onboarding Guide](./ONBOARDING.md) - Complete guide for new developers
- [Contributing Guide](./CONTRIBUTING.md) - Development workflow and standards
- [Project Constitution](./.claude/context/project-constitution.md) - Development principles and practices

### Architecture & Design
- [Architecture Overview](./docs/docs/architecture/overview.md) - System design and decisions
- [Architecture Diagrams](./ARCHITECTURE_DIAGRAMS.md) - Visual system architecture with Mermaid diagrams
- [API Reference](./API_REFERENCE.md) - Complete REST API documentation
- [API Documentation](./API_DOCUMENTATION.md) - Legacy API reference
- [Interactive API Docs](http://localhost:8080/swagger-ui.html) - Try out the API in your browser

### Development Resources
- [Glossary](./GLOSSARY.md) - Project terminology and definitions
- [Security Policy](./SECURITY.md) - Security practices and vulnerability reporting
- [System Documentation](./docs/) - Complete Docusaurus documentation site
- [llms.txt](./llms.txt) - AI context file for automated tools

## 🛡️ Security

Security is a top priority. Please see our [Security Policy](./SECURITY.md) for:
- Reporting vulnerabilities
- Security best practices
- Compliance standards (OWASP, PCI DSS, GDPR)

## 📄 License

This project is licensed under the [LICENSE](./LICENSE) file - see the LICENSE file for details.

## 🆘 Support

- [Troubleshooting Guide](./TROUBLESHOOTING.md)
- [Issue Tracker](https://github.com/your-organization/sass/issues)
- [Security Issues](./SECURITY.md)

For questions about the constitutional development approach, check out our [Project Constitution](./.claude/context/project-constitution.md).

---

<div align="center">

**SASS** - Building secure, modular applications with AI-assisted development practices

[Back to top](#sass---spring-boot-application-with-security-system)

</div>
