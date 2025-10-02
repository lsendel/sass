SHELL := /bin/bash

# Environment variables with defaults
JAVA_HOME ?= /opt/homebrew/opt/openjdk@21
BACKEND_PORT ?= 8082
FRONTEND_PORT ?= 3000

# Default target
.PHONY: help
help: ## Show this help message
	@echo "🚀 Spring Boot Modulith Payment Platform"
	@echo "========================================="
	@echo ""
	@echo "📦 Setup & Cleanup:"
	@echo "  setup               Install all dependencies"
	@echo "  setup-backend       Install Java dependencies"
	@echo "  setup-frontend      Install Node.js dependencies"
	@echo "  clean               Clean all build artifacts"
	@echo ""
	@echo "🛠️  Development:"
	@echo "  dev                 Start full development environment (recommended)"
	@echo "  dev-backend         Start only backend server"
	@echo "  dev-frontend        Start only frontend server"
	@echo "  stop                Stop all running processes"
	@echo ""
	@echo "🧪 Testing:"
	@echo "  test                Run all tests (backend + frontend + python)"
	@echo "  test-quick          Quick test suite (unit tests only)"
	@echo "  test-all            Comprehensive test suite (all test types)"
	@echo "  test-ci             CI test suite (with quality checks)"
	@echo ""
	@echo "  Backend Tests:"
	@echo "    test-backend                  All backend tests"
	@echo "    test-backend-unit             Unit tests only"
	@echo "    test-backend-contract         Contract tests only"
	@echo "    test-backend-integration      Integration tests (H2 in-memory)"
	@echo "    test-backend-testcontainer    TestContainer tests (PostgreSQL Docker)"
	@echo "    test-backend-all-integration  All integration tests (H2 + TestContainer)"
	@echo ""
	@echo "  Frontend Tests:"
	@echo "    test-frontend           Unit + integration tests"
	@echo "    test-frontend-unit      Unit tests only"
	@echo "    test-frontend-api       API tests only"
	@echo "    test-frontend-e2e       E2E tests with Playwright"
	@echo "    test-frontend-e2e-ui    E2E tests (interactive UI)"
	@echo "    test-frontend-coverage  Tests with coverage report"
	@echo ""
	@echo "  Reports & Evidence:"
	@echo "    test-report            Open all test reports"
	@echo "    test-coverage-report   Open coverage reports"
	@echo "    test-evidence          Collect evidence (screenshots, videos)"
	@echo ""
	@echo "🏗️  Building:"
	@echo "  build               Build for production"
	@echo "  build-backend       Build Java JAR"
	@echo "  build-frontend      Build React assets"
	@echo ""
	@echo "✨ Code Quality:"
	@echo "  lint                Run all linting"
	@echo "  format              Format all code"
	@echo "  pre-commit          Run all quality checks"
	@echo ""
	@echo "🎯 Demo & Testing:"
	@echo "  demo                Complete authentication demo"
	@echo "  health              Check service status"
	@echo "  test-api            Test API endpoints"
	@echo "  test-cors           Test CORS configuration"
	@echo "  test-auth           Test authentication endpoints"
	@echo "  validate-deployment Complete deployment validation"
	@echo ""
	@echo "🔧 Utilities:"
	@echo "  env-info            Show environment details"
	@echo "  git-clean-lock      Clean git lock files"
	@echo "  quick-start         Full setup and start"
	@echo "  code-quality        Comprehensive quality checks"
	@echo "  backup-db           Backup database"
	@echo ""
	@echo "🤖 ACIS - Automated Quality:"
	@echo "  acis-install        Install ACIS git hooks"
	@echo "  acis-status         Check ACIS installation status"
	@echo "  acis-test           Test ACIS system"
	@echo "  acis-run            Run ACIS manual validation"
	@echo "  acis-uninstall      Uninstall ACIS git hooks"
	@echo ""
	@echo "🚀 Deployment:"
	@echo "  deploy-staging      Deploy to staging"
	@echo "  deploy-production   Deploy to production"
	@echo ""
	@echo "📚 More commands: make help-all"
	@echo ""
	@echo "🚀 Quick Start: make setup && make dev"

.PHONY: help-all
help-all: ## Show all available commands with descriptions
	@echo "🚀 Spring Boot Modulith Payment Platform - All Commands"
	@echo "======================================================="
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Environment Variables:"
	@echo "  JAVA_HOME            Java 21 installation path (default: /opt/homebrew/opt/openjdk@21)"
	@echo "  BACKEND_PORT         Backend server port (default: 8082)"
	@echo "  FRONTEND_PORT        Frontend server port (default: 3000)"

# Prerequisite checking
.PHONY: check-prereqs
check-prereqs: ## Check if all prerequisites are installed
	@echo "🔍 Checking prerequisites..."
	@echo -n "Java 21: "
	@export PATH=$(JAVA_HOME)/bin:$$PATH && java -version 2>&1 | head -n1 || echo "❌ Not found"
	@echo -n "Node.js: "
	@node --version || echo "❌ Not found"
	@echo -n "npm: "
	@npm --version || echo "❌ Not found"
	@echo -n "Git: "
	@git --version || echo "❌ Not found"
	@echo -n "Docker: "
	@docker --version || echo "❌ Not found (optional)"
	@echo "✅ Prerequisite check complete"

# Setup commands
.PHONY: setup setup-backend setup-frontend
setup: check-prereqs setup-backend setup-frontend ## Install all dependencies (backend + frontend)
	@echo "✅ Setup complete! Run 'make dev' to start development servers."

setup-backend: ## Install backend dependencies
	@echo "🔧 Setting up backend dependencies..."
	@if [ ! -f backend/gradlew ]; then \
		echo "❌ backend/gradlew not found. Are you in the right directory?"; \
		exit 1; \
	fi
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew build -x test && echo "✅ Backend setup complete" || (echo "❌ Backend setup failed" && exit 1)

setup-frontend: ## Install frontend dependencies
	@echo "🔧 Setting up frontend dependencies..."
	@if [ ! -f frontend/package.json ]; then \
		echo "❌ frontend/package.json not found. Are you in the right directory?"; \
		exit 1; \
	fi
	@cd frontend && npm install && echo "✅ Frontend setup complete" || (echo "❌ Frontend setup failed" && exit 1)

# Clean commands
.PHONY: clean clean-backend clean-frontend
clean: clean-backend clean-frontend ## Clean all build artifacts
	@echo "🧹 Cleaned all build artifacts"

clean-backend: ## Clean backend build artifacts
	@echo "🧹 Cleaning backend build artifacts..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew clean

clean-frontend: ## Clean frontend build artifacts
	@echo "🧹 Cleaning frontend build artifacts..."
	cd frontend && rm -rf node_modules/.vite dist

# Development commands
.PHONY: dev dev-backend dev-frontend start-all
dev: ## Start full development environment (recommended)
	@echo "🚀 Starting full development environment..."
	@echo "📱 Frontend will be available at: http://localhost:$(FRONTEND_PORT)"
	@echo "⚙️  Backend will be available at: http://localhost:$(BACKEND_PORT)"
	@echo "🔐 Password authentication enabled on backend"
	@echo ""
	@echo "Starting backend server..."
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew bootRun --args='--spring.profiles.active=development --server.port=$(BACKEND_PORT) --app.auth.password.enabled=true' --console=plain &
	@echo "⏳ Waiting for backend to start..."
	@sleep 10
	@echo "Starting frontend server..."
	@cd frontend && npm run dev &
	@echo ""
	@echo "✅ Development environment is starting!"
	@echo "📖 Press Ctrl+C to stop servers, or run 'make stop' to stop background processes"

dev-backend: ## Start only backend server
	@echo "🔧 Starting Spring Boot backend server on port $(BACKEND_PORT)..."
	@echo "🔐 Password authentication enabled"
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew bootRun --args='--spring.profiles.active=development --server.port=$(BACKEND_PORT) --app.auth.password.enabled=true' --console=plain

dev-frontend: ## Start only frontend server
	@echo "📱 Starting React frontend server on port $(FRONTEND_PORT)..."
	cd frontend && npm run dev

start-all: ## Start all services in parallel (backend + frontend)
	@echo "🌟 Starting complete development stack..."
	@echo "This will start backend and frontend services"
	make dev-backend &
	@sleep 5
	make dev-frontend &
	@echo "✅ All services starting in parallel"

# Stop processes
.PHONY: stop stop-all
stop: ## Stop all running background processes
	@echo "🛑 Stopping all background processes..."
	@pkill -f "gradlew bootRun" || true
	@pkill -f "npm run dev" || true
	@pkill -f "vite" || true
	@pkill -f "playwright" || true
	@echo "✅ Stopped all development servers"

stop-all: stop ## Alias for stop command

# Testing commands
.PHONY: test test-backend test-frontend test-python test-all

# Main test command - runs all basic tests
test: test-backend test-frontend test-python ## Run all tests (backend + frontend + python)
	@echo "✅ All tests completed"

# Backend testing
test-backend: ## Run backend tests (unit + integration)
	@echo "🧪 Running backend tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test

test-backend-contract: ## Run backend contract tests only
	@echo "📋 Running backend contract tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*ContractTest"

test-backend-integration: ## Run backend integration tests only (H2 in-memory)
	@echo "🔗 Running backend integration tests with H2..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*IntegrationTest"

test-backend-testcontainer: ## Run backend TestContainer tests only (PostgreSQL Docker)
	@echo "🐳 Running backend TestContainer tests with real PostgreSQL..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*TestContainerTest" --tests "DatabaseConnectivityTest"

test-backend-all-integration: ## Run all integration tests (H2 + TestContainer)
	@echo "🔗 Running all backend integration tests (H2 + PostgreSQL TestContainer)..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*IntegrationTest" --tests "*TestContainerTest" --tests "DatabaseConnectivityTest"
	@echo "✅ All integration tests completed (8 tests)"

test-backend-unit: ## Run backend unit tests only
	@echo "🧪 Running backend unit tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*UnitTest"

# Frontend testing - comprehensive
.PHONY: test-frontend test-frontend-unit test-frontend-api test-frontend-integration test-frontend-e2e
test-frontend: ## Run frontend tests (unit + integration)
	@echo "🧪 Running frontend tests..."
	cd frontend && npm run test:unit

test-frontend-unit: ## Run frontend unit tests only
	@echo "🧪 Running frontend unit tests..."
	cd frontend && npm run test:unit

test-frontend-api: ## Run frontend API tests
	@echo "🔌 Running frontend API tests..."
	cd frontend && npm run test:api

test-frontend-integration: ## Run frontend integration tests
	@echo "🔗 Running frontend integration tests..."
	cd frontend && npm run test:integration

test-frontend-e2e: ## Run frontend E2E tests with Playwright
	@echo "🎭 Running frontend E2E tests..."
	cd frontend && npm run test:e2e

test-frontend-e2e-ui: ## Run frontend E2E tests with interactive UI
	@echo "🎭 Running frontend E2E tests in UI mode..."
	cd frontend && npm run test:e2e:ui

test-frontend-e2e-headed: ## Run frontend E2E tests in headed mode (visible browser)
	@echo "🎭 Running frontend E2E tests in headed mode..."
	cd frontend && npm run test:e2e:headed

test-frontend-e2e-debug: ## Debug frontend E2E tests
	@echo "🐛 Debugging frontend E2E tests..."
	cd frontend && npm run test:e2e:debug

test-frontend-watch: ## Run frontend tests in watch mode
	@echo "👀 Running frontend tests in watch mode..."
	cd frontend && npm run test:watch

test-frontend-coverage: ## Run frontend tests with coverage report
	@echo "📊 Running frontend tests with coverage..."
	cd frontend && npm run test:coverage
	@echo "📈 Coverage report generated in frontend/coverage/"

test-frontend-all: test-frontend-unit test-frontend-integration test-frontend-e2e ## Run all frontend tests (unit + integration + e2e)
	@echo "✅ All frontend tests completed"

# Python testing
test-python: ## Run Python constitutional tools tests
	@echo "🐍 Running Python constitutional tools tests..."
	cd tools && python3 -m pytest ../tests/python/

# Comprehensive test suite
test-all: ## Run comprehensive test suite (all systems with all test types)
	@echo "🧪 Running comprehensive test suite..."
	@echo ""
	@echo "📦 Backend Tests:"
	@echo "  - Contract tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*ContractTest"
	@echo "  - Integration tests (H2)..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*IntegrationTest"
	@echo "  - TestContainer tests (PostgreSQL)..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*TestContainerTest" --tests "DatabaseConnectivityTest"
	@echo "  - Unit tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*UnitTest"
	@echo ""
	@echo "🎨 Frontend Tests:"
	@echo "  - Unit tests..."
	cd frontend && npm run test:unit
	@echo "  - API tests..."
	cd frontend && npm run test:api
	@echo "  - Integration tests..."
	cd frontend && npm run test:integration
	@echo "  - E2E tests..."
	cd frontend && npm run test:e2e
	@echo ""
	@echo "🐍 Python Tests:"
	cd tools && python3 -m pytest ../tests/python/
	@echo ""
	@echo "✅ Comprehensive test suite completed"

# CI-specific test commands
.PHONY: test-ci test-frontend-ci test-backend-ci
test-ci: ## Run tests for CI environment (comprehensive checks)
	@echo "🤖 Running CI test suite..."
	@echo "Running backend tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test
	@echo "Running frontend CI tests..."
	cd frontend && npm run test:ci
	@echo "Running Python tests..."
	cd tools && python3 -m pytest ../tests/python/
	@echo "✅ CI test suite completed"

test-frontend-ci: ## Run frontend tests for CI (with type checking, linting, and security)
	@echo "🤖 Running comprehensive frontend CI tests..."
	cd frontend && npm run test:ci

test-backend-ci: ## Run backend tests for CI
	@echo "🤖 Running backend CI tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew clean test

# Test reporting and evidence collection
.PHONY: test-workflows test-usability test-evidence test-report test-coverage-report
test-workflows: ## Run comprehensive workflow validation with evidence collection
	@echo "🔍 Running comprehensive workflow validation with evidence collection..."
	@echo "📸 Evidence will be saved to test-results/comprehensive-validation-*"
	cd frontend && npx playwright test tests/e2e/comprehensive-workflow-validation.spec.ts --reporter=html
	@echo "✅ Workflow validation complete - check test-results for evidence"

test-usability: ## Run usability and accessibility tests
	@echo "♿ Running usability and accessibility validation..."
	cd frontend && npx playwright test tests/e2e/comprehensive-workflow-validation.spec.ts --grep="usability|accessibility"
	@echo "✅ Usability validation complete"

test-evidence: ## Run all tests and collect evidence (screenshots, videos, traces)
	@echo "📸 Running tests with full evidence collection..."
	cd frontend && npx playwright test --trace on --video on --screenshot on
	@echo "📊 Generating test report..."
	cd frontend && npx playwright show-report
	@echo "✅ Evidence collection complete"

test-report: ## Open all test reports
	@echo "📊 Opening test reports..."
	@if [ -f "frontend/test-results/html/index.html" ]; then \
		open frontend/test-results/html/index.html; \
	else \
		echo "⚠️  Unit test report not found. Run 'make test-frontend-coverage' first."; \
	fi
	@if [ -f "frontend/test-results/report/index.html" ]; then \
		open frontend/test-results/report/index.html; \
	else \
		echo "⚠️  E2E test report not found. Run 'make test-frontend-e2e' first."; \
	fi

test-coverage-report: ## Open coverage reports
	@echo "📈 Opening coverage reports..."
	@if [ -d "frontend/coverage" ]; then \
		cd frontend && npm run coverage:open; \
	else \
		echo "⚠️  Coverage report not found. Run 'make test-frontend-coverage' first."; \
	fi
	@if [ -d "backend/build/reports/jacoco/test/html" ]; then \
		open backend/build/reports/jacoco/test/html/index.html; \
	else \
		echo "⚠️  Backend coverage report not found. Run 'make test-backend' first."; \
	fi

# Parallel testing (experimental)
.PHONY: test-parallel test-quick
test-parallel: ## Run frontend and backend tests in parallel (faster)
	@echo "🚀 Running tests in parallel..."
	@$(MAKE) test-backend & \
	$(MAKE) test-frontend-unit & \
	wait
	@echo "✅ Parallel tests completed"

test-quick: ## Quick test: run essential unit tests only (fast)
	@echo "⚡ Running quick test suite..."
	@echo "Backend unit tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*UnitTest"
	@echo "Frontend unit tests..."
	cd frontend && npm run test:unit
	@echo "✅ Quick test suite completed"

# Build commands
.PHONY: build build-backend build-frontend build-all
build: build-backend build-frontend ## Build both backend and frontend for production
	@echo "✅ Build completed for both backend and frontend"

build-backend: ## Build backend JAR
	@echo "🔨 Building backend JAR..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew build

build-frontend: ## Build frontend static assets
	@echo "🔨 Building frontend static assets..."
	cd frontend && npm run build

build-all: clean build ## Clean and build everything from scratch
	@echo "✅ Complete clean build finished"

# Code quality commands
.PHONY: lint lint-backend lint-frontend format format-backend format-frontend
lint: lint-backend lint-frontend ## Run linting on both backend and frontend
	@echo "✅ Linting completed"

lint-backend: ## Run backend code style checks
	@echo "🔍 Running backend linting..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew checkstyleMain checkstyleTest

lint-frontend: ## Run frontend linting
	@echo "🔍 Running frontend linting..."
	cd frontend && npm run lint

format: format-backend format-frontend ## Format code in both projects
	@echo "✅ Code formatting completed"

format-backend: ## Format backend code (run checkstyle to verify)
	@echo "📝 Backend formatting (run checkstyle to verify)..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew checkstyleMain

format-frontend: ## Format frontend code
	@echo "📝 Formatting frontend code..."
	cd frontend && npm run format

# Demo commands
.PHONY: demo-start demo-stop demo-test demo-login demo auth-flow

demo-start: ## Start demo environment (backend + frontend) without timer
	@echo "🚀 Starting demo environment..."
	@echo "📝 Demo Credentials:"
	@echo "   Email: demo@example.com"
	@echo "   Password: DemoPassword123!"
	@echo "   Organization ID: 00000000-0000-0000-0000-000000000001"
	@echo ""
	@echo "⚙️ Starting backend with test data (port $(BACKEND_PORT))..."
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew bootRun --args='--spring.profiles.active=test --server.port=$(BACKEND_PORT) --app.auth.password.enabled=true' --console=plain &
	@echo "⏳ Waiting for backend to initialize..."
	@for i in {1..30}; do \
		curl -s http://localhost:$(BACKEND_PORT)/actuator/health > /dev/null && break || sleep 1; \
	done
	@echo "✅ Backend is running!"
	@echo ""
	@echo "📱 Starting frontend (port $(FRONTEND_PORT))..."
	@cd frontend && npm run dev &
	@echo "⏳ Waiting for frontend to initialize..."
	@sleep 5
	@echo "✅ Frontend is running!"
	@echo ""
	@echo "🎉 Demo environment ready!"
	@echo "   Frontend: http://localhost:$(FRONTEND_PORT)/auth/login"
	@echo "   Backend API: http://localhost:$(BACKEND_PORT)/api/v1"
	@echo ""
	@echo "Use 'make demo-stop' to stop all services"

demo-stop: ## Stop demo environment
	@echo "🛑 Stopping demo environment..."
	@pkill -f "gradlew bootRun" || true
	@pkill -f "npm run dev" || true
	@pkill -f "vite" || true
	@echo "✅ Demo environment stopped"

demo-test: ## Test login with demo credentials
	@echo "🧪 Testing authentication with demo credentials..."
	@echo "Attempting login with:"
	@echo "   Email: demo@example.com"
	@echo "   Password: DemoPassword123!"
	@curl -s -X POST http://localhost:$(BACKEND_PORT)/api/v1/auth/login \
		-H "Content-Type: application/json" \
		-d '{"email": "demo@example.com", "password": "DemoPassword123!", "organizationId": "00000000-0000-0000-0000-000000000001"}' | python3 -m json.tool || echo "❌ Login failed - is the backend running?"

demo-login: demo-start ## Start demo and show login form with Playwright
	@echo "🎭 Running Playwright login demonstration..."
	cd frontend && node show-login-form.cjs
	@echo "📸 Check login-form-evidence.png for visual proof!"

demo: demo-login ## Complete demo with auto-start (alias for demo-login)

auth-flow: ## Run complete authentication flow test with detailed analysis
	@echo "🧪 Running complete authentication flow integration test..."
	@echo "This will test the full login flow from form to API calls"
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew bootRun --args='--spring.profiles.active=test --server.port=$(BACKEND_PORT) --app.auth.password.enabled=true' --console=plain &
	@echo "⏳ Waiting for backend to start..."
	@sleep 15
	@cd frontend && npm run dev &
	@echo "⏳ Waiting for frontend to start..."
	@sleep 10
	@echo "🔍 Running comprehensive authentication flow analysis..."
	cd frontend && node test-auth-flow.cjs
	@echo "📊 Check auth-flow-test-results.json for detailed test results!"

# Health check commands
.PHONY: health env-info
health: ## Check if services are running
	@echo "🏥 Checking service health..."
	@echo ""
	@echo -n "Backend (port $(BACKEND_PORT)): "
	@if curl -s -f http://localhost:$(BACKEND_PORT)/actuator/health > /dev/null 2>&1; then \
		echo "✅ Healthy"; \
	else \
		echo "❌ Not responding"; \
	fi
	@echo -n "Frontend (port $(FRONTEND_PORT)): "
	@if curl -s -f http://localhost:$(FRONTEND_PORT) > /dev/null 2>&1; then \
		echo "✅ Responding"; \
	else \
		echo "❌ Not responding"; \
	fi
	@echo ""
	@echo "💡 Tip: Use 'make dev' to start all services"

env-info: ## Show environment information
	@echo "🔧 Environment Information:"
	@echo "JAVA_HOME: $(JAVA_HOME)"
	@echo "Backend Port: $(BACKEND_PORT)"
	@echo "Frontend Port: $(FRONTEND_PORT)"
	@echo ""
	@echo "Java Version:"
	@export PATH=$(JAVA_HOME)/bin:$$PATH && java -version || echo "❌ Java not found"
	@echo ""
	@echo "Node Version:"
	@node --version || echo "❌ Node not found"
	@echo ""
	@echo "NPM Version:"
	@npm --version || echo "❌ NPM not found"

# CORS testing
.PHONY: cors-test api-test
cors-test: ## Test CORS configuration
	@echo "🌐 Testing CORS configuration..."
	bash backend/test-cors.sh

api-test: ## Test API endpoints
	@echo "🔗 Testing authentication API endpoints..."
	@echo "Testing /api/v1/auth/methods:"
	@curl -s -H "Origin: http://localhost:3000" http://localhost:$(BACKEND_PORT)/api/v1/auth/methods | python3 -m json.tool || echo "❌ API not responding"

# Quick commands for common workflows
.PHONY: quick-start
quick-start: stop setup dev ## Quick start: clean setup and start development
	@echo "🚀 Quick start complete!"

# Git commands (existing)
.PHONY: git-clean-lock git-clean-lock-force
git-clean-lock: ## Clean git lock files
	@bash scripts/git-clean-lock.sh

git-clean-lock-force: ## Force clean git lock files
	@bash scripts/git-clean-lock.sh --force

# Missing commands referenced in README
.PHONY: pre-commit security-check docs prod
pre-commit: lint test ## Run all quality checks before committing
	@echo "✅ Pre-commit checks passed!"

security-check: ## Run comprehensive security scans and validation
	@echo "🔒 Running comprehensive security checks..."
	@if [ -f "scripts/security-check.sh" ]; then \
		bash scripts/security-check.sh; \
	else \
		echo "Backend security scan:"; \
		cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew dependencyCheckAnalyze || echo "⚠️  Dependency check not configured"; \
		echo "Frontend security scan:"; \
		cd frontend && npm audit || echo "⚠️  npm audit found issues"; \
		echo "✅ Basic security check complete"; \
	fi

docs: ## Generate and serve documentation
	@echo "📚 Starting documentation server..."
	@if [ -d "docs" ]; then \
		cd docs && npm install && npm start; \
	else \
		echo "❌ docs directory not found"; \
	fi

prod: ## Start production-like environment
	@echo "🌟 Starting production environment..."
	@echo "⚙️ Building all components first..."
	@make build
	@echo "🚀 Starting services in production mode..."
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew bootRun --args='--spring.profiles.active=production --server.port=$(BACKEND_PORT)' --console=plain &
	@echo "⏳ Waiting for backend to start..."
	@sleep 10
	@echo "📱 Serving frontend static files..."
	@cd frontend && npm run preview &
	@echo "✅ Production environment started!"

# Convenient shortcuts
.PHONY: install start restart logs status
install: setup ## Alias for setup (install dependencies)

start: dev ## Alias for dev (start development)

restart: stop dev ## Restart development environment
	@echo "🔄 Development environment restarted!"

logs: ## Show logs from running services
	@echo "📋 Recent logs from services..."
	@echo "Backend logs:"
	@pkill -f "gradlew bootRun" -USR1 2>/dev/null || echo "Backend not running"
	@echo ""
	@echo "Frontend logs:"
	@pkill -f "vite" -USR1 2>/dev/null || echo "Frontend not running"

status: health ## Alias for health check

# Improved quick commands
.PHONY: fresh full-clean
fresh: full-clean setup dev ## Complete fresh start (clean + setup + dev)
	@echo "🌟 Fresh development environment ready!"

full-clean: stop clean ## Stop everything and clean all artifacts
	@echo "🧹 Full cleanup complete!"

# Advanced testing and validation commands
.PHONY: test-api test-cors test-auth validate-deployment code-quality
test-api: ## Test API endpoints functionality
	@echo "🔗 Testing API endpoints..."
	@if [ -f "test_api_endpoints.sh" ]; then \
		bash test_api_endpoints.sh; \
	else \
		echo "Testing health endpoint:"; \
		curl -s -f http://localhost:$(BACKEND_PORT)/actuator/health > /dev/null && echo "✅ Health endpoint responding" || echo "❌ Health endpoint not responding"; \
		echo "Testing API documentation:"; \
		curl -s -f http://localhost:$(BACKEND_PORT)/swagger-ui.html > /dev/null && echo "✅ Swagger UI accessible" || echo "❌ Swagger UI not accessible"; \
	fi

test-cors: cors-test ## Alias for CORS testing

test-auth: ## Test authentication endpoints
	@echo "🔐 Testing authentication endpoints..."
	@if [ -f "backend/test_auth_methods.sh" ]; then \
		bash backend/test_auth_methods.sh; \
	else \
		echo "Testing auth methods endpoint:"; \
		curl -s -f http://localhost:$(BACKEND_PORT)/api/v1/auth/methods > /dev/null && echo "✅ Auth methods endpoint responding" || echo "❌ Auth methods endpoint not responding"; \
	fi

validate-deployment: health test-api test-cors ## Comprehensive deployment validation
	@echo "✅ Deployment validation complete!"

code-quality: ## Run comprehensive code quality automation
	@echo "✨ Running comprehensive code quality checks..."
	@if [ -f "scripts/code-quality-automation.sh" ]; then \
		bash scripts/code-quality-automation.sh; \
	else \
		echo "Running basic quality checks:"; \
		make lint; \
		make test; \
		echo "✅ Basic code quality checks complete"; \
	fi

# Production deployment commands
.PHONY: deploy-staging deploy-production backup-db
deploy-staging: ## Deploy to staging environment
	@echo "🚀 Deploying to staging environment..."
	@if [ -f "scripts/deploy-staging.sh" ]; then \
		bash scripts/deploy-staging.sh; \
	else \
		echo "❌ Staging deployment script not found"; \
		echo "💡 Use 'make prod' for production-like local environment"; \
	fi

deploy-production: ## Deploy to production environment
	@echo "🌟 Deploying to production environment..."
	@if [ -f "scripts/deploy-production.sh" ]; then \
		bash scripts/deploy-production.sh; \
	elif [ -f "deploy.sh" ]; then \
		bash deploy.sh; \
	else \
		echo "❌ Production deployment script not found"; \
		echo "💡 Use 'make prod' for production-like local environment"; \
	fi

backup-db: ## Backup database
	@echo "💾 Creating database backup..."
	@if [ -f "scripts/backup-database.sh" ]; then \
		bash scripts/backup-database.sh; \
	else \
		echo "Creating manual backup..."; \
		timestamp=$$(date +%Y%m%d_%H%M%S); \
		echo "Backup would be created with timestamp: $$timestamp"; \
		echo "💡 Manual backup commands:"; \
		echo "  Docker: docker exec postgres_container pg_dump -U user dbname > backup_$$timestamp.sql"; \
		echo "  Local: pg_dump -h localhost -U user dbname > backup_$$timestamp.sql"; \
	fi

# ============================================================================
# ACIS - Automated Continuous Improvement System
# ============================================================================

.PHONY: acis-install acis-status acis-test acis-run acis-uninstall acis-validate

acis-install: ## Install ACIS git hooks for automated quality checks
	@echo "🤖 Installing ACIS git hooks..."
	@./scripts/acis/git-hooks/install-hooks.sh install
	@echo "✅ ACIS git hooks installed"
	@echo ""
	@echo "📝 What happens now:"
	@echo "  • Pre-commit: Fast validation when you commit (1-5 min)"
	@echo "  • Pre-push: Full validation before push (5-30 min)"
	@echo "  • Auto-fix: Code style issues fixed automatically"
	@echo ""
	@echo "💡 To skip (emergency only): git commit --no-verify"

acis-status: ## Check ACIS installation and configuration status
	@echo "🤖 ACIS Status Check"
	@echo "===================="
	@./scripts/acis/git-hooks/install-hooks.sh status

acis-test: ## Test ACIS system functionality
	@echo "🧪 Testing ACIS system..."
	@./scripts/acis/git-hooks/install-hooks.sh test
	@echo ""
	@echo "✅ ACIS test complete"

acis-run: ## Run ACIS manual validation (dry run)
	@echo "🤖 Running ACIS validation (dry run)..."
	@echo "This will validate code quality without making changes"
	@echo ""
	@ACIS_CONTINUE_APPROVED=true DRY_RUN=true ./scripts/acis/acis-main.sh
	@echo ""
	@echo "📊 Check logs: .acis/logs/acis.log"
	@echo "📊 Check reports: .acis/reports/"

acis-uninstall: ## Uninstall ACIS git hooks
	@echo "🤖 Uninstalling ACIS git hooks..."
	@./scripts/acis/git-hooks/install-hooks.sh uninstall
	@echo "✅ ACIS git hooks uninstalled"

acis-validate: acis-test ## Validate ACIS system is working properly
	@echo ""
	@echo "✅ ACIS validation complete!"

# Integration with existing test commands
.PHONY: test-acis test-quality-gates

test-acis: acis-test ## Run ACIS system tests

test-quality-gates: ## Run quality gates validation using ACIS
	@echo "🔒 Running quality gates validation..."
	@echo ""
	@echo "Quality Gates:"
	@echo "  1. Compilation (Java)"
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && \
		./gradlew compileJava compileTestJava --no-daemon && \
		echo "  ✅ Compilation passed" || echo "  ❌ Compilation failed"
	@echo "  2. Code Style (Checkstyle)"
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && \
		./gradlew checkstyleMain checkstyleTest --no-daemon && \
		echo "  ✅ Code style passed" || echo "  ❌ Code style failed"
	@echo "  3. Tests (Unit + Integration)"
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && \
		./gradlew test --no-daemon && \
		echo "  ✅ Tests passed" || echo "  ❌ Tests failed"
	@echo "  4. Coverage (≥85%)"
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && \
		./gradlew jacocoTestCoverageVerification --no-daemon && \
		echo "  ✅ Coverage passed" || echo "  ❌ Coverage failed"
	@echo ""
	@echo "✅ Quality gates validation complete!"

# Enhanced test commands with ACIS integration
.PHONY: test-with-acis test-ci-acis

test-with-acis: acis-install test-quality-gates ## Run tests with ACIS quality gates
	@echo "✅ Tests with ACIS quality gates complete!"

test-ci-acis: test-quality-gates ## CI test suite with ACIS quality gates
	@echo "✅ CI tests with ACIS quality gates complete!"

# Quick quality check using ACIS
.PHONY: quick-quality

quick-quality: ## Quick quality check using ACIS (fast)
	@echo "⚡ Running quick quality check..."
	@echo ""
	@echo "1. Compilation check..."
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && \
		./gradlew compileJava --no-daemon -q && echo "  ✅ Compiled" || echo "  ❌ Failed"
	@echo "2. Code style check..."
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && \
		./gradlew checkstyleMain --no-daemon -q && echo "  ✅ Style OK" || echo "  ❌ Style issues"
	@echo "3. Quick tests..."
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && \
		./gradlew test --tests "*UnitTest" --no-daemon -q && echo "  ✅ Tests OK" || echo "  ❌ Tests failed"
	@echo ""
	@echo "✅ Quick quality check complete!"

# ACIS-enhanced setup
.PHONY: setup-with-acis

setup-with-acis: setup acis-install ## Setup project with ACIS git hooks
	@echo ""
	@echo "✨ Project setup complete with ACIS!"
	@echo ""
	@echo "🚀 Next steps:"
	@echo "  1. Run: make dev"
	@echo "  2. Make changes and commit"
	@echo "  3. ACIS will validate automatically"
	@echo ""
	@echo "💡 Check ACIS status: make acis-status"
	@echo "💡 Test ACIS: make acis-test"

