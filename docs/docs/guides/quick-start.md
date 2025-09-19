# Quick Start Guide

Get up and running with the Spring Boot Modulith Payment Platform in minutes.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** (OpenJDK recommended)
- **Node.js 18+** for frontend development
- **Docker** for TestContainers and local services
- **PostgreSQL** and **Redis** (can be run via Docker)

## Backend Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/sass-platform/sass.git
   cd sass
   ```

2. **Start the backend**:
   ```bash
   cd backend
   ./gradlew bootRun --args='--spring.profiles.active=test'
   ```

3. **Run tests**:
   ```bash
   ./gradlew test
   ./gradlew test --tests "*ArchitectureTest"
   ```

## Frontend Setup

1. **Install dependencies**:
   ```bash
   cd frontend
   npm install
   ```

2. **Start the development server**:
   ```bash
   npm run dev
   ```

3. **Run tests**:
   ```bash
   npm run test
   npm run test:e2e
   ```

## Documentation Development

1. **Start the documentation server**:
   ```bash
   cd docs
   npm install
   npm run start
   ```

2. **Build documentation**:
   ```bash
   npm run build
   ```

## Next Steps

- [Architecture Overview](../architecture/overview.md)
- [Backend Development Guide](../backend/getting-started.md)
- [Frontend Development Guide](../frontend/getting-started.md)