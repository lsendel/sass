# Repository Guidelines

## Project Structure & Module Organization
- `backend/` — Spring Boot (Gradle, Java 21). App code in `src/main/java`, tests in `src/test/java`.
- `frontend/` — React + Vite + TypeScript. Source in `src/`, tests in `tests/`.
- `src/` — Python utilities and agents (architecture, security, development helpers).
- `tests/` — Python test suites (`unit/`, `integration/`, `e2e/`, `agent/`, `contract/`).
- `scripts/` — Repo utilities (e.g., `git-clean-lock.sh`).
- Ops: `docker-compose.yml`, `k8s/`, `monitoring/`, `.env.example`.

## Build, Test, and Development Commands
- Infra: `docker compose up -d` — Postgres, Redis, Mailhog, Adminer.
- Backend: `cd backend && ./gradlew bootRun` (run), `./gradlew test` (JUnit + JaCoCo), `./gradlew build` (fat JAR).
- Frontend: `cd frontend && npm install && npm run dev` (serve), `npm run build`, `npm test` (Vitest), `npm run test:e2e` (Playwright).
- Python: `pytest -q` (from repo root) to run agent/architecture suites.
- Utilities: `make git-clean-lock` to remove a stale Git `index.lock`.

## Coding Style & Naming Conventions
- Java: Checkstyle enforced (`backend/config/checkstyle/checkstyle.xml`). Package root `com.platform.*`.
- TypeScript/React: ESLint + Prettier (`frontend/.eslintrc.cjs`, `.prettierrc`). Components PascalCase, tests `*.test.ts(x)`.
- Python: Follow PEP 8. Place code under `src/<module>/` and tests under `tests/<suite>/test_*.py`.
- Indentation: 2 spaces (TS/JS), 4 spaces (Java/Python). Keep functions small and cohesive.

## Testing Guidelines
- Backend: `./gradlew test jacocoTestReport`; coverage HTML at `backend/build/jacocoHtml/index.html`.
- Frontend: `npm run test:coverage`; E2E requires `npm run test:e2e:install` once.
- Python: target fast unit tests first; mark slow/integration appropriately.

## Commit & Pull Request Guidelines
- Use Conventional Commits where possible: `feat: ...`, `fix(ci): ...`, `docs: ...`.
- PRs must include: clear description, linked issues (`Closes #123`), test evidence (logs or screenshots for UI), and any config changes (.env, compose).
- CI must pass lint + tests for backend, frontend, and Python.

## Security & Configuration Tips
- Never commit secrets. Start from `.env.example` or `.env.production.example`.
- Prefer configuration via env vars; document additions in the PR.

## Agent-Specific Instructions
- Do not rename or move modules without discussion.
- Respect existing tools (Gradle, ESLint/Prettier, pytest). Keep changes minimal and focused.
