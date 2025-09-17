SHELL := /bin/bash

# Environment variables with defaults
JAVA_HOME ?= /opt/homebrew/opt/openjdk@21
BACKEND_PORT ?= 8082
FRONTEND_PORT ?= 3000

# Default target
.PHONY: help
help: ## Show this help message
	@echo "Spring Boot Modulith Payment Platform - Development Commands"
	@echo "============================================================"
	@echo ""
	@echo "Setup Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "setup\|clean" | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Development Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "dev\|start\|stop" | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Testing Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "test" | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Build Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "build" | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Code Quality Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "lint\|format" | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Demo & Utility Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "demo\|health\|env\|cors" | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Git Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | grep "git" | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
	@echo ""
	@echo "Environment Variables:"
	@echo "  JAVA_HOME            Java 21 installation path (default: /opt/homebrew/opt/openjdk@21)"
	@echo "  BACKEND_PORT         Backend server port (default: 8082)"
	@echo "  FRONTEND_PORT        Frontend server port (default: 3000)"
	@echo ""
	@echo "Quick Start:"
	@echo "  make dev             Start full development environment (recommended)"
	@echo "  make demo            Run login form demonstration"
	@echo "  make health          Check if services are running"

# Setup commands
.PHONY: setup setup-backend setup-frontend
setup: setup-backend setup-frontend ## Install all dependencies (backend + frontend)
	@echo "✅ Setup complete! Run 'make dev' to start development servers."

setup-backend: ## Install backend dependencies
	@echo "🔧 Setting up backend dependencies..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew build -x test

setup-frontend: ## Install frontend dependencies
	@echo "🔧 Setting up frontend dependencies..."
	cd frontend && npm install

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
	@cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew bootRun --args='--spring.profiles.active=test --server.port=$(BACKEND_PORT) --app.auth.password.enabled=true' --console=plain &
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
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew bootRun --args='--spring.profiles.active=test --server.port=$(BACKEND_PORT) --app.auth.password.enabled=true' --console=plain

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
.PHONY: test test-backend test-frontend test-all
test: test-backend test-frontend ## Run all tests (backend + frontend)
	@echo "✅ All tests completed"

test-backend: ## Run backend tests (unit + integration)
	@echo "🧪 Running backend tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test

test-frontend: ## Run frontend tests (unit + e2e)
	@echo "🧪 Running frontend tests..."
	cd frontend && npm run test

test-all: ## Run comprehensive test suite
	@echo "🧪 Running comprehensive test suite..."
	@echo "Running backend contract tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*ContractTest"
	@echo "Running backend integration tests..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*IntegrationTest"
	@echo "Running frontend unit tests..."
	cd frontend && npm run test
	@echo "Running frontend E2E tests..."
	cd frontend && npm run test:e2e
	@echo "✅ Comprehensive test suite completed"

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
	@echo "Backend (port $(BACKEND_PORT)):"
	@curl -s http://localhost:$(BACKEND_PORT)/actuator/health || echo "❌ Backend not responding"
	@echo ""
	@echo "Frontend (port $(FRONTEND_PORT)):"
	@curl -s http://localhost:$(FRONTEND_PORT) > /dev/null && echo "✅ Frontend responding" || echo "❌ Frontend not responding"

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
.PHONY: quick-start quick-test
quick-start: stop setup dev ## Quick start: clean setup and start development
	@echo "🚀 Quick start complete!"

quick-test: ## Quick test: run essential tests only
	@echo "⚡ Running quick test suite..."
	cd backend && export JAVA_HOME=$(JAVA_HOME) && export PATH=$(JAVA_HOME)/bin:$$PATH && ./gradlew test --tests "*UnitTest"
	cd frontend && npm run test -- --run

# Git commands (existing)
.PHONY: git-clean-lock git-clean-lock-force
git-clean-lock: ## Clean git lock files
	@bash scripts/git-clean-lock.sh

git-clean-lock-force: ## Force clean git lock files
	@bash scripts/git-clean-lock.sh --force

