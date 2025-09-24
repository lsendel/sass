# SASS Architecture Overview

## Dual-Stack Architecture

SASS (Spring Boot Application with Security System) implements a unique dual-stack architecture combining:

1. **Application Stack** (Java/TypeScript)
   - Spring Boot backend with modular architecture
   - React/TypeScript frontend with modern tooling
   - Comprehensive testing with JUnit and Playwright

2. **Constitutional Tools Stack** (Python)
   - AI-powered development assistance agents
   - Constitutional enforcement system
   - Development workflow automation

## Application Stack

### Backend (Java)
- **Framework**: Spring Boot 3.5.5 with Java 21
- **Architecture**: Modular monolith using Spring Modulith
- **Modules**:
  - `auth`: Authentication and authorization
  - `payment`: Payment processing with Stripe
  - `subscription`: Subscription management
  - `user`: User management
  - `audit`: Audit logging
  - `shared`: Common utilities
- **Testing**: JUnit 5, Testcontainers, REST Assured
- **Build**: Gradle with JaCoCo coverage, Checkstyle, SonarQube

### Frontend (TypeScript)
- **Framework**: React with Vite
- **Styling**: Tailwind CSS
- **Testing**: Vitest for unit tests, Playwright for E2E
- **Build**: Modern bundling with hot reload

## Constitutional Tools Stack

### Overview
The constitutional tools provide AI-powered development assistance and enforcement of development practices. This is a separate Python system that complements the main application.

### Components
- **Agents**: Specialized AI agents for different enforcement tasks
  - Constitutional Enforcement Agent
  - TDD Compliance Agent
  - Task Coordination Agent
- **Architecture Validation**: Module boundary and dependency enforcement
- **Security Compliance**: OWASP, PCI DSS, GDPR validation
- **Workflow Orchestration**: Multi-agent coordination

### Technology Stack
- **Language**: Python 3.9+
- **Testing**: pytest with coverage reporting
- **Code Quality**: black, isort, flake8, mypy
- **Build**: setuptools with pyproject.toml

## Directory Structure

```
sass/
├── backend/           # Java Spring Boot application
│   ├── src/main/java/ # Application code
│   └── src/test/java/ # Java tests
├── frontend/          # React/TypeScript application
│   ├── src/          # Application code
│   └── tests/        # Frontend tests
├── tools/            # Python constitutional tools
│   ├── agents/       # AI agents
│   ├── workflows/    # Workflow orchestration
│   └── requirements.txt
├── tests/            # Test organization
│   ├── java/         # Java application tests (symlink to backend)
│   └── python/       # Python tools tests
├── docs/             # Documentation
├── k8s/              # Kubernetes manifests
├── monitoring/       # Monitoring configuration
└── scripts/          # Utility scripts
```

## Development Workflow

### Application Development
1. Write tests first (TDD)
2. Implement features in Java/TypeScript
3. Run comprehensive test suite
4. Deploy via CI/CD pipeline

### Tools Development
1. Develop agents and workflows in Python
2. Test constitutional enforcement
3. Integrate with application development process
4. Provide development assistance

## Testing Strategy

### Application Tests
- **Unit Tests**: Individual components
- **Integration Tests**: Module interactions
- **Contract Tests**: API contracts
- **E2E Tests**: Full user workflows

### Constitutional Tests
- **Agent Tests**: Individual agent functionality
- **Contract Tests**: Agent API contracts
- **E2E Tests**: Complete constitutional workflows
- **Integration Tests**: Agent coordination

## CI/CD Pipelines

### Application Pipeline
- Backend CI: Java build, test, coverage
- Frontend CI: TypeScript build, test, lint
- E2E CI: Cross-browser testing
- Security scans and dependency checks

### Tools Pipeline
- Python CI: Multi-version testing, linting, type checking
- Coverage reporting and quality gates
- Integration with main application pipeline

## Benefits of Dual-Stack

1. **Separation of Concerns**: Application logic vs. development tooling
2. **Technology Appropriateness**: Java for application, Python for AI/ML tooling
3. **Independent Evolution**: Tools can evolve separately from application
4. **Specialized Testing**: Appropriate testing frameworks for each stack
5. **Developer Experience**: Best tools for each domain

## Communication Between Stacks

- **File System**: Shared configuration and documentation
- **APIs**: RESTful communication for integration points
- **Events**: Event-driven architecture for loose coupling
- **CI/CD**: Coordinated deployment and testing

## Future Considerations

- **Microservices Migration**: Potential split of Java modules
- **Tool Integration**: Deeper integration of constitutional tools
- **Cross-Platform**: Support for additional languages/frameworks
- **AI Enhancement**: Expanded AI capabilities in development workflow
