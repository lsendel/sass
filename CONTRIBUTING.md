# Contributing to SASS

Thank you for your interest in contributing to the SASS (Spring Boot Application with Security System) project! This document outlines the guidelines and processes for contributing to this repository.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Development Setup](#development-setup)
- [Project Architecture](#project-architecture)
- [Code Standards](#code-standards)
- [Testing Strategy](#testing-strategy)
- [Documentation](#documentation)
- [Pull Request Process](#pull-request-process)
- [Constitutional Compliance](#constitutional-compliance)
- [Security Guidelines](#security-guidelines)

## Prerequisites

Before contributing, ensure you have the following installed:

- Java 21 (OpenJDK recommended)
- Node.js 18+ and npm/yarn
- Python 3.9+
- Docker and Docker Compose
- Git

## Development Setup

1. **Fork and Clone the Repository**
   ```bash
   git clone https://github.com/your-username/sass.git
   cd sass
   ```

2. **Install Dependencies**
   ```bash
   # Install all dependencies
   make setup
   ```

3. **Start Development Environment**
   ```bash
   # Start all services
   make dev
   ```

## Project Architecture

The SASS project implements a **dual-stack architecture**:

- **Application Stack**: Java Spring Boot backend + React/TypeScript frontend
- **Constitutional Tools Stack**: Python-based AI agents for development enforcement

For detailed architecture information, see [ARCHITECTURE.md](./ARCHITECTURE.md).

### Backend (Java)

- Framework: Spring Boot 3.5.5
- Architecture: Modular monolith using Spring Modulith
- Key modules: `auth`, `payment`, `subscription`, `user`, `audit`, `shared`

### Frontend (TypeScript)

- Framework: React with Vite
- Styling: Tailwind CSS
- Testing: Vitest for unit tests, Playwright for E2E

### Constitutional Tools (Python)

- AI-powered development assistance agents
- Constitutional enforcement system
- Development workflow automation

## Code Standards

### Java

- Follow Google Java Style Guide
- Use meaningful variable and method names
- Write Javadoc for public APIs
- Keep methods focused and short where possible

### TypeScript

- Follow TypeScript/JavaScript Style Guide
- Use TypeScript type annotations consistently
- Follow React best practices
- Write TSDoc for public APIs

### Python

- Follow PEP 8 style guide
- Use meaningful variable and function names
- Write docstrings for functions and classes
- Follow the project's Python conventions

## Testing Strategy

### Backend Testing

```bash
# Run Java tests
make test-backend
```

Testing includes:
- Unit tests with JUnit 5
- Integration tests with Testcontainers
- Contract tests
- Security tests

### Frontend Testing

```bash
# Run TypeScript tests
make test-frontend
```

Testing includes:
- Unit tests with Vitest
- E2E tests with Playwright
- Component tests

### Constitutional Tools Testing

```bash
# Run Python tests
make test-python
```

Testing includes:
- Unit tests with pytest
- Agent contract tests
- Workflow integration tests

### Running All Tests

```bash
# Run all tests (Java + TypeScript + Python)
make test
```

## Documentation

### Required Documentation

- API endpoints must have OpenAPI annotations
- Public methods/classes must have documentation
- Architectural decisions must be documented as ADRs
- Security-sensitive code must include security notes

### Documentation Structure

- Top-level: README.md, CONTRIBUTING.md, SECURITY.md, GLOSSARY.md
- Code-level: JavaDoc, TSDoc, Python docstrings
- Architecture: docs/architecture/
- Operations: docs/ops/
- Quality: docs/quality/

## Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Follow the code standards
   - Write/modify tests as appropriate
   - Update documentation as needed
   - Ensure all tests pass

3. **Run pre-commit checks**
   ```bash
   make pre-commit
   ```

4. **Commit your changes**
   - Follow conventional commit format
   - Include issue references if applicable
   ```bash
   git commit -m "feat(auth): add OAuth2 provider configuration #123"
   ```

5. **Push and create pull request**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Wait for review**
   - Address reviewer feedback
   - Ensure CI checks pass
   - Verify all required tests pass

## Constitutional Compliance

This project follows a constitutional development approach. All contributions must comply with the project's constitution:

- **TDD Compliance**: Tests must be written before implementation
- **Modular Architecture**: Respect module boundaries and dependencies
- **Security First**: All code must consider security implications
- **Documentation**: All public APIs must be documented
- **Quality Gates**: All quality checks must pass before merging

### Constitutional Enforcement

The project uses AI agents to enforce development practices:

- **Constitutional Enforcement Agent**: Ensures adherence to project constitution
- **TDD Compliance Agent**: Verifies test-first approach
- **Architecture Validation Agent**: Enforces module boundaries
- **Security Compliance Agent**: Validates security practices

## Security Guidelines

### Security Considerations

- All user inputs must be validated and sanitized
- Authentication and authorization must be properly implemented
- Security headers must be included
- Secrets must be properly handled and never hardcoded
- Audit logging must be implemented for sensitive operations

### Reporting Security Issues

Security issues should be reported privately. See [SECURITY.md](./SECURITY.md) for details.

## Getting Help

- Check the documentation in the `docs/` directory
- Look for examples in the existing codebase
- Ask questions in pull requests or issues
- Review the [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) guide

## Development Workflow

### Daily Development

1. Sync with main branch:
   ```bash
   git checkout main
   git pull origin main
   ```

2. Create feature branch:
   ```bash
   git checkout -b feature/my-feature
   ```

3. Make changes and commit:
   ```bash
   # Make your changes
   git add .
   git commit -m "Your commit message"
   ```

4. Test your changes:
   ```bash
   make test
   ```

5. Push and submit PR:
   ```bash
   git push origin feature/my-feature
   ```

### Best Practices

- Write tests first (TDD approach)
- Keep pull requests small and focused
- Update documentation when implementing new features
- Ensure all tests pass before submitting PR
- Be responsive to review comments

## Code Review Process

All code changes must be reviewed by at least one other contributor. Reviewers should check for:

- Code quality and adherence to standards
- Test coverage and quality
- Security implications
- Performance considerations
- Architectural alignment
- Documentation completeness

## License

By contributing to this project, you agree that your contributions will be licensed under the same license as the project.