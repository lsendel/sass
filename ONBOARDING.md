# SASS Developer Onboarding Guide

Welcome to the SASS (Spring Boot Application with Security System) project! This guide will help you get up and running as quickly as possible.

## Table of Contents

1. [Project Overview](#project-overview)
2. [Prerequisites](#prerequisites)
3. [Development Environment Setup](#development-environment-setup)
4. [Project Architecture](#project-architecture)
5. [Development Workflow](#development-workflow)
6. [Key Technologies](#key-technologies)
7. [Running the Application](#running-the-application)
8. [Testing](#testing)
9. [Documentation](#documentation)
10. [Troubleshooting](#troubleshooting)
11. [Getting Help](#getting-help)

## Project Overview

SASS is a comprehensive payment platform built with a dual-stack architecture:

- **Application Stack**: Java Spring Boot backend with React/TypeScript frontend
- **Constitutional Tools Stack**: Python-based AI agents for development enforcement

### Key Features
- Secure authentication with OAuth2
- Payment processing with Stripe
- Multi-tenant organization support
- Audit logging for compliance
- AI-assisted development practices

## Prerequisites

Before you begin, ensure you have the following installed:

- **Git** (2.30+)
- **Java 21** (OpenJDK recommended)
- **Node.js** (18+)
- **npm or yarn** (latest versions)
- **Python 3.9+**
- **Docker** and **Docker Compose**
- **IDE/Editor** (IntelliJ IDEA, VS Code, or similar)

### Verification Steps

Check that your tools are properly installed:

```bash
# Check Git
git --version

# Check Java
java -version
javac -version

# Check Node.js
node --version
npm --version

# Check Python
python3 --version

# Check Docker
docker --version
docker-compose --version
```

## Development Environment Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-organization/sass.git
cd sass
```

### 2. Install Dependencies

The project uses a Makefile to manage common tasks:

```bash
# Install all dependencies for all parts of the project
make setup
```

This command will:
- Install backend dependencies (Gradle will download required JARs)
- Install frontend dependencies (npm/yarn install)
- Install Python dependencies (pip install -r requirements.txt)

### 3. Configure Environment

Copy the example environment files:

```bash
# For development
cp .env.example .env

# For production (when needed)
cp .env.production.example .env.production
```

The `.env` file contains default settings for local development. You may need to add actual API keys for external services like Stripe and OAuth providers when working with those features.

## Project Architecture

### Application Stack

```
├── backend/           # Java Spring Boot application
│   ├── src/main/java/com/platform/
│   │   ├── auth/      # Authentication module
│   │   ├── payment/   # Payment processing module
│   │   ├── subscription/ # Subscription management
│   │   ├── user/      # User management
│   │   └── shared/    # Shared utilities and security
│   └── src/test/java/ # Java tests
├── frontend/          # React/TypeScript application
│   ├── src/
│   │   ├── components/ # Reusable React components
│   │   ├── pages/     # Application pages
│   │   ├── store/     # Redux state management
│   │   └── api/       # API clients and services
└── tools/             # Python constitutional tools
    ├── agents/        # AI enforcement agents
    └── workflows/     # Multi-agent coordination
```

### Constitutional Tools Stack

The constitutional tools provide AI-powered development assistance:

- **Constitutional Enforcement Agent**: Ensures adherence to project constitution
- **TDD Compliance Agent**: Verifies test-driven development practices
- **Architecture Validation Agent**: Enforces module boundaries
- **Security Compliance Agent**: Validates security practices

## Development Workflow

### 1. Understanding the Process

The SASS project follows a constitutional development approach:

1. **Plan**: Define requirements and architecture
2. **Code**: Implement following TDD practices
3. **Test**: Ensure adequate test coverage
4. **Document**: Update relevant documentation
5. **Review**: Submit for peer review
6. **Constitutional Check**: AI agents validate compliance

### 2. Branch Strategy

- `main`: Production-ready code
- `develop`: Integration branch for features
- `feature/*`: Feature development branches
- `hotfix/*`: Urgent fixes for production
- `release/*`: Release preparation branches

### 3. Making Changes

```bash
# Create a feature branch
git checkout -b feature/my-awesome-feature

# Make your changes following TDD practices
# 1. Write tests first
# 2. Implement code to pass tests
# 3. Refactor as needed

# Run pre-commit checks
make pre-commit

# Commit your changes
git add .
git commit -m "feat: add my awesome feature"
```

## Key Technologies

### Backend (Java/Spring Boot)

- **Spring Boot 3.5.5**: Main application framework
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: Data persistence
- **Spring Modulith**: Modular architecture
- **Spring WebFlux**: Reactive web programming
- **Testcontainers**: Integration testing with Docker
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework

### Frontend (React/TypeScript)

- **React 19**: Component-based UI library
- **TypeScript**: Type-safe JavaScript
- **Vite**: Fast build tool
- **Tailwind CSS**: Utility-first CSS framework
- **Redux Toolkit**: State management
- **RTK Query**: API client with caching
- **Vitest**: Fast test runner
- **Playwright**: E2E testing

### Infrastructure

- **PostgreSQL**: Primary database
- **Redis**: Caching and session storage
- **Docker**: Containerization
- **Kubernetes**: Orchestration (production)
- **Stripe**: Payment processing
- **OAuth2**: Authentication providers

## Running the Application

### 1. Local Development

```bash
# Start the complete application stack
make dev
```

This will:
- Start PostgreSQL and Redis with Docker Compose
- Build and start the backend application
- Build and start the frontend application
- Make both available for development

### 2. Component-Specific Commands

#### Backend Only
```bash
# Navigate to backend directory
cd backend

# Run with Gradle
./gradlew bootRun

# Or use the Makefile
make backend-dev
```

#### Frontend Only
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies and run
npm install
npm run dev

# Or use the Makefile
make frontend-dev
```

### 3. Application URLs

After starting the application:

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8082
- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **Actuator Health**: http://localhost:8082/actuator/health

## Testing

### 1. Test Structure

```
├── backend/src/test/java/     # Java unit and integration tests
├── frontend/tests/          # Frontend tests
│   ├── unit/                # Unit tests (Vitest)
│   └── e2e/                 # E2E tests (Playwright)
└── tests/python/            # Python constitutional tool tests
```

### 2. Running Tests

```bash
# Run all tests
make test

# Run backend tests only
make test-backend

# Run frontend tests only
make test-frontend

# Run Python/constitutional tool tests
make test-python

# Run specific test suites
cd backend && ./gradlew test
cd frontend && npm test
cd tools && python -m pytest ../tests/python/
```

### 3. Test Coverage

The project maintains a target of 85%+ test coverage. You can check coverage with:

```bash
# Backend coverage
make coverage-backend

# Frontend coverage
make coverage-frontend
```

## Documentation

### 1. Documentation Structure

```
├── README.md              # Project overview
├── CONTRIBUTING.md        # Contribution guidelines
├── SECURITY.md            # Security policy
├── GLOSSARY.md            # Project terminology
├── API_DOCUMENTATION.md   # API reference
├── ARCHITECTURE.md        # High-level architecture
├── DEPLOYMENT.md          # Deployment guide
├── docs/                  # Docusaurus documentation site
└── llms.txt               # AI context file
```

### 2. Updating Documentation

When making code changes, update the corresponding documentation:

- API changes → Update API documentation
- Architecture changes → Update architecture docs
- Configuration changes → Update setup guides
- New features → Add to README and relevant guides

### 3. Documentation Commands

```bash
# Build documentation site
make docs

# Serve documentation locally
cd docs && npm run start
```

## Troubleshooting

### Common Issues

#### 1. Port Conflicts
```
Error: Address already in use
```

Solution: Check if another process is using the required ports (3000, 8082, 5432, 6379).

```bash
# Check port usage
lsof -i :3000
lsof -i :8082
lsof -i :5432
lsof -i :6379

# Kill processes if needed
kill -9 <PID>
```

#### 2. Database Connection Issues
```
Error: Connection to PostgreSQL failed
```

Solution: Ensure Docker services are running:

```bash
# Check running containers
docker-compose ps

# Restart database services
docker-compose down && docker-compose up -d postgres redis
```

#### 3. Node.js Memory Issues
```
FATAL ERROR: Reached heap limit Allocation failed
```

Solution: Increase Node.js memory limit:

```bash
export NODE_OPTIONS="--max-old-space-size=4096"
```

#### 4. Python Dependencies
```
ModuleNotFoundError: No module named 'xxx'
```

Solution: Reinstall Python dependencies:

```bash
cd tools
pip install -r requirements.txt
```

### Useful Commands

```bash
# Clean and rebuild everything
make clean-all && make setup

# Reset database
make reset-db

# Check system status
make status

# View logs
make logs
```

## Getting Help

### 1. Documentation
- Start with the [README.md](./README.md) for project overview
- Check [CONTRIBUTING.md](./CONTRIBUTING.md) for development guidelines
- Review [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) for common issues

### 2. Contact Channels
- **Slack/Teams**: Join the #sass-dev channel
- **GitHub Issues**: Report bugs or ask questions
- **Email**: Contact the development team

### 3. Common Resources
- [Project Architecture](./docs/docs/architecture/overview.md)
- [API Documentation](./API_DOCUMENTATION.md)
- [Security Guidelines](./SECURITY.md)
- [Glossary of Terms](./GLOSSARY.md)

### 4. Understanding the Constitution

The project follows constitutional development practices:

- **TDD Required**: Tests must be written before implementation
- **Security First**: Security considerations in every feature
- **Documentation Required**: Public APIs must be documented
- **Modular Boundaries**: Respect architectural boundaries
- **Quality Gates**: All checks must pass before merging

## Next Steps

1. Explore the codebase structure
2. Run the application locally
3. Look at existing tests to understand patterns
4. Join the development team communication channels
5. Pick up a beginner-friendly issue to work on
6. Follow the [Contributing Guide](./CONTRIBUTING.md)

## Development Tips

1. **Start Small**: Begin with documentation fixes or small bug fixes
2. **Run Tests**: Always run relevant tests before submitting changes
3. **Follow Patterns**: Look at existing code to understand project patterns
4. **Ask Questions**: Don't hesitate to ask for clarification
5. **Incremental Changes**: Make small, focused commits
6. **Review Others**: Participate in code reviews to learn the codebase

Welcome aboard! 🎉

If you have any questions not covered in this guide, reach out to the development team. We're here to help you succeed!