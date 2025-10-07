# Prompt: End-to-End Documentation Plan for This Repository (LLM-Ready)

You are an experienced technical writer, software architect, and security engineer. Using only the code in this repository and the guidance and best practices below, produce complete, accurate, security-first documentation for code, application behavior, and architecture. Follow the plan, deliverables, templates, and quality bar exactly. Where uncertainty exists, propose options and flags for human review.

## Repository Snapshot (Context You Must Parse)

- Tech stack
  - Backend: Java, Spring Boot (OAuth2, multi-tenant context, Stripe webhook)
  - Frontend: React + TypeScript (Vite, Vitest, Playwright)
  - Agents/Infra Logic: Python modules under `src/` (security, development gates, agents)
  - DevOps: Docker Compose, Kubernetes manifests in `k8s/`, monitoring/, deployment script `deploy.sh`
- Key directories to crawl and model
  - `backend/src/main/java/com/platform/*` (auth, payment, subscription, user, shared/security)
  - `frontend/src/*` (pages, components, store slices, API clients, tests)
  - `src/*` (agents, security, architecture, development gates)
  - `tests/`, `frontend/tests/e2e/*`, `backend/src/test/*`
  - `.env.example`, `.env.production.example`, `docker-compose*.yml`, `k8s/*`, `monitoring/*`

## Objectives

- Generate high-quality documentation for this codebase with multi-layer validation and human-review checkpoints.
- Cover code-level docs, API/interface docs, application behavior, security, and multi-view architecture.
- Establish a repeatable, CI-friendly documentation workflow tailored to this repo.

## Deliverables (Create/Update)

- Top-level
  - `README.md`: concise product overview, quickstart, architecture summary, security highlights
  - `CONTRIBUTING.md`: contribution flow, review gates, doc-update policy, pre-commit checks
  - `SECURITY.md`: disclosure policy, dependencies risk policy, scanning requirements
  - `GLOSSARY.md`: business/domain terms (subscription, plan, invoice, tenant, etc.)
  - `llms.txt`: durable context file for LLMs (rules, tech, directory map, invariants)
- Code/API documentation
  - Java: Javadoc-style docstrings for controllers, services, entities; endpoint catalog; error codes; auth and tenant context notes
  - TypeScript/React: TS doc comments for public components, hooks, store slices; state shapes; error/reporting patterns
  - Python: docstrings for modules in `src/agents`, `src/security`, `src/architecture`, `src/development`; purpose, contracts, invariants
  - API Reference: inferred from Spring controllers; include routes, methods, params, schemas, auth requirements, error handling
  - Error Catalog: standardized error codes/messages; mapping to HTTP status; recovery/UX guidance
- Architecture documentation (under `docs/architecture/`)
  - Overview: context, goals, constraints, quality attributes
  - Multi-view:
    - Application View (capabilities, bounded contexts: auth, user/org, subscription, payments)
    - Development View (modules, packages, layering, boundaries, cross-cutting concerns)
    - Security View (authN/Z flows, tenant isolation, token handling, webhook verification, data protection)
    - Infrastructure View (runtime topology, Docker/K8s, networking, external services: Stripe)
    - Sizing/Performance View (throughput, latency SLOs, env sizing, test data)
  - ADRs: key decisions (e.g., OAuth2 provider choice, tenant context, Stripe webhooks, Redux pattern, testing stack)
  - Diagrams: sequence, component, deployment, data flow (use Mermaid in docs)
- Operations docs (under `docs/ops/`)
  - Deployment runbooks (compose, k8s), rollback, secrets management
  - Monitoring/alerting overview; health endpoints; SLIs/SLOs
  - Backup, data retention, privacy notes
- QA & Validation (under `docs/quality/`)
  - Review checklists, test strategy, coverage targets, mutation testing plan
  - Doc-to-test validation plan and results artifact

## Process & Workflow (Follow Step-By-Step)

1. Build durable context
   - Map repository: modules, ownership, dependencies, entry points, generated artifacts
   - Extract domain vocabulary and invariants from code, tests, and configs
2. Generate baseline documentation
   - Populate all deliverables using templates below
   - Create diagrams from inferred relationships; link to code references
3. Validate documentation via tests
   - From docs, synthesize runnable tests (unit/integration/e2e assertions) to verify described behavior and contracts
   - Run tests and flag any doc-code mismatches; propose corrections
4. Security-first review
   - Threat model key flows: login/OAuth2, tenant routing, payments and webhooks, token persistence
   - Identify input validation, secrets, headers, crypto, access control; document gaps and mitigations
5. Human review gates
   - Present diffs, open questions, and high-uncertainty areas; apply the checklists
6. Iterate and finalize
   - Resolve inconsistencies; update diagrams; create ADRs for major decisions
7. CI/CD integration plan
   - Propose GitHub Actions (or equivalent) jobs for doc generation, linting, broken-link checking, and drift detection

## Quality Bar (Acceptance Criteria)

- Accuracy: content matches actual code, configs, and runtime behavior
- Traceability: every API/diagram links to concrete code locations
- Security: sensitive flows documented with controls, residual risks, and test proofs
- Consistency: standardized templates, terminology, and style across files
- Maintainability: clear update rules; automated checks; low drift risk

## Templates You Must Use

### API Documentation Template

```markdown
## API Name and Description

**API Name:** [Clear, concise name]
**Description:** [Purpose, core benefits, and value proposition]

## Technical Specifications

- **Input Parameters:** [Detailed parameter descriptions]
- **Output Format:** [Response structure and data types]
- **Error Handling:** [Expected error codes and messages]
- **Authentication:** [Security requirements]

## Usage Examples

[Code examples with expected outputs]

## Integration Guidelines

[Context-specific implementation notes]
```

### Code Documentation Template (Java/TS/Python)

```markdown
/\*\*

- Purpose: [Explain the "why" - business logic, design decisions]
- Parameters: [Type and purpose of each parameter]
- Returns: [Return value and format]
- Throws: [Potential exceptions and when they occur]
- Example: [Usage example with expected output]
- Security Notes: [Any security considerations]
  \*/
```

### Architecture Document Outline

```markdown
# System Overview

- Mission, scope, stakeholders, constraints, quality attributes

# Views

- Application View: capabilities, contexts, domain model
- Development View: modules, layering, boundaries, dependencies
- Security View: authN/Z, secrets, data protection, threat model
- Infrastructure View: environments, topology, K8s, networking
- Sizing/Performance View: SLIs/SLOs, sizing, capacity assumptions

# Decisions (ADRs)

- [ADR-001] Title: Context, options, decision, consequences

# Diagrams

- Component, sequence, deployment, data flow (Mermaid)
```

### Security Documentation Checklist (Apply Repo-Specifics)

- Authentication and session management (OAuth2 success handler, token stores)
- Tenant isolation (filters, context propagation, data access controls)
- Input validation and sanitization (controllers, request models)
- Webhook verification (Stripe signatures, replay protection)
- Security headers, HTTPS, CORS, CSRF strategies
- Secrets and configuration management (.env, K8s secrets)
- Data encryption at rest/in transit; PII handling, logging redaction
- Authorization rules (endpoints, roles, organization membership)
- Audit logging (events, retention)

## Review Checklists

- Business logic accuracy
- Security vulnerability assessment
- Integration compatibility (frontend-backend, Stripe, OAuth2)
- Documentation-code alignment
- Error handling completeness and consistency

## Validation & Metrics

- Documentation completeness score and unresolved TODOs
- Accuracy verified by doc-generated tests (pass/fail list)
- Coverage deltas on critical flows
- Developer feedback items and time-to-onboard metric
- Drift detection: doc vs. code changes per PR

## CI/CD Documentation Workflow (Propose and Configure)

- Pre-commit: LLM doc linter, comment completeness checks for changed files
- CI jobs:
  - Generate/refresh docs for changed modules
  - SAST/secret scan; dependency audit; SBOM attach
  - Doc test synthesis and execution for changed endpoints/components
  - Broken link and Mermaid render checks
- PR Gates: require docs updated for API/signature changes; require security notes for sensitive diffs

## Repo-Specific Coverage Requirements

- Backend (Spring Boot)
  - Document controllers: `AuthController`, `UserController`, `OrganizationController`, `PlanController`, `SubscriptionController`, `PaymentController`, `StripeWebhookController`
  - Entities and value objects: `Subscription`, `Plan`, `Payment`, `Invoice`, `Money`, `Email`
  - Security: `SecurityConfig`, `OpaqueTokenAuthenticationFilter`, `CustomOAuth2UserService`, `OAuth2AuthenticationSuccessHandler`, `TenantContext`
  - Persistence: repositories and converters; transaction boundaries
  - Error handling: exception mapping; HTTP status contracts
- Frontend (React + TS)
  - Pages: Auth, Dashboard, Subscription, Payments, Organizations
  - Store: RTK query APIs (`userApi`, `authApi`, `subscriptionApi`, `paymentApi`, `organizationApi`); slices (`authSlice`, `uiSlice`)
  - Components: modals, layouts, error boundaries; loading states and UX contracts
  - Tests: Vitest unit tests, Playwright e2e flows; map tests to documented user journeys
- Python Agents/Guards (`src/`)
  - Intent and enforcement points: `token_enforcer`, `compliance_enforcer`, `implementation_gate`, `tdd_compliance`, `documentation`, `task_coordination`
  - Eventing and boundaries: `event_coordinator`, `module_boundaries`
  - Contracts, invariants, and extension points
- DevOps
  - Deployment: `docker-compose.yml`, `docker-compose.prod.yml`, `k8s/*`, `deploy.sh`
  - Monitoring: document provided dashboards/probes; health endpoints
  - Configuration: `.env.example`, `.env.production.example`; required variables and secrets handling

## Style & Editorial Guidelines

- Write for engineers first; summarize, then detail
- Prefer active voice, declarative statements, and code-linked references
- Keep sections scannable: short paragraphs, lists, and tables where helpful
- Use consistent terminology aligned to the `GLOSSARY.md`
- Call out assumptions and open questions explicitly

## Open Questions to Resolve (If Evidence Is Ambiguous)

- OAuth2 providers and scopes; token lifetimes and storage specifics
- Stripe product/price mapping to `Plan` and invoicing schedules
- Tenant routing and data partitioning strategy in persistence
- Production monitoring stack and SLOs

## Best Practices Basis (Do Not Reproduce Verbatim; Use As Guidance)

- Multi-layer validation for doc accuracy via test execution
- Mandatory human review focused on business logic, security, integrations, and edge cases
- Multi-view architecture documentation with ADRs and diagrams
- CI-integrated, continuous documentation with drift detection
- Security-first documentation: auth, inputs, headers, crypto, access control
- Context-aware, durable prompts and `llms.txt` to reduce hallucinations
- Progressive documentation: AI drafts refined by humans

## Selected References

- Identifying inaccurate descriptions in LLM-generated comments via test execution (arXiv:2406.14836)
- Security and quality in LLM-generated code (arXiv:2502.01853)
- GitHub: Code review in the age of AI (human ownership of merge)
- RepoAgent: repository-level code documentation generation
- Architecture document template (multi-view)
- Honeycomb and Simon Willison on durable context and workflows
- SonarSource AI standards and LLMs for code generation
- Microsoft DocAider, GitBook AI docs workflows
- Overviews of AI code documentation tools and templates (Mintlify, Graphite)

---

Instructions: Use this prompt to produce the full documentation set. Embed code references, generate diagrams, synthesize doc-derived tests, and prepare CI steps. Where necessary, add ADRs and propose security mitigations with clear, actionable next steps.
