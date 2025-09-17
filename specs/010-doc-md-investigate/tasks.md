# Tasks: Comprehensive Project Documentation System

**Input**: Design documents from `/specs/010-doc-md-investigate/`
**Prerequisites**: plan.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

## Execution Flow (main)
```
1. Load plan.md from feature directory ✓
   → Tech stack: Docusaurus 2, Node.js 18+, TypeScript
   → Libraries: docs-generator, docs-validator, docs-create
   → Structure: Web app (frontend/backend + docs/)
2. Load design documents:
   → data-model.md: 7 entities (DocumentationPage, Section, etc.)
   → contracts/: documentation-api.yaml, cli-contracts.md
   → research.md: Docusaurus 2 selection, deployment strategy
3. Generate tasks by category:
   → Setup: Docusaurus init, dependencies, structure
   → Tests: API contract tests, CLI tests, validation tests
   → Core: CLI libraries, validation, templates
   → Integration: Build pipeline, search, versioning
   → Content: Migration, templates, validation
4. Apply task rules:
   → Library files = [P] parallel
   → Contract tests = [P] parallel
   → Same documentation = sequential
5. Number tasks sequentially (T001-T039)
6. Generate dependency graph
7. Validate completeness ✓
8. Return: SUCCESS (39 tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- File paths assume web app structure with docs/ subdirectory

## Phase 3.1: Infrastructure Setup
- [ ] T001 Create docs/ directory structure with Docusaurus 2 configuration
- [ ] T002 Initialize Node.js project in docs/ with TypeScript and documentation dependencies
- [ ] T003 [P] Configure ESLint, Prettier, and markdown linting tools in docs/
- [ ] T004 [P] Create GitHub Actions workflow for documentation validation in .github/workflows/docs.yml
- [ ] T005 [P] Configure pre-commit hooks for documentation validation in .pre-commit-config.yaml
- [ ] T006 Create documentation content structure in docs/docs/ (architecture/, backend/, frontend/, guides/)
- [ ] T007 [P] Configure Algolia DocSearch for documentation search in docs/docusaurus.config.js
- [ ] T008 [P] Set up static asset directories and image optimization in docs/static/

## Phase 3.2: Contract Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] T009 [P] Contract test GET /api/docs/pages in docs/tests/contract/test_pages_get.test.ts
- [ ] T010 [P] Contract test POST /api/docs/pages in docs/tests/contract/test_pages_post.test.ts
- [ ] T011 [P] Contract test GET /api/docs/search in docs/tests/contract/test_search.test.ts
- [ ] T012 [P] Contract test GET /api/docs/sections in docs/tests/contract/test_sections.test.ts
- [ ] T013 [P] Contract test docs-build CLI interface in docs/tests/contract/test_cli_build.test.ts
- [ ] T014 [P] Contract test docs-validate CLI interface in docs/tests/contract/test_cli_validate.test.ts
- [ ] T015 [P] Contract test docs-serve CLI interface in docs/tests/contract/test_cli_serve.test.ts
- [ ] T016 [P] Contract test docs-create CLI interface in docs/tests/contract/test_cli_create.test.ts

## Phase 3.3: Library Development (ONLY after tests are failing)
- [ ] T017 [P] DocumentationPage entity schema validation in src/lib/docs-validator/src/schemas/page.ts
- [ ] T018 [P] DocumentationSection entity schema validation in src/lib/docs-validator/src/schemas/section.ts
- [ ] T019 [P] SearchIndex entity schema validation in src/lib/docs-validator/src/schemas/search.ts
- [ ] T020 [P] DocumentationTemplate entity processing in src/lib/docs-generator/src/templates/index.ts
- [ ] T021 [P] docs-build CLI command implementation in src/lib/docs-generator/src/cli/build.ts
- [ ] T022 [P] docs-validate CLI command implementation in src/lib/docs-validator/src/cli/validate.ts
- [ ] T023 [P] docs-serve CLI command implementation in src/lib/docs-generator/src/cli/serve.ts
- [ ] T024 [P] docs-create CLI command implementation in src/lib/docs-generator/src/cli/create.ts

## Phase 3.4: API Implementation
- [ ] T025 GET /api/docs/pages endpoint with filtering and pagination in docs/src/api/pages.ts
- [ ] T026 POST /api/docs/pages endpoint with validation in docs/src/api/pages.ts
- [ ] T027 GET /api/docs/search endpoint with full-text search in docs/src/api/search.ts
- [ ] T028 GET /api/docs/sections endpoint with navigation tree in docs/src/api/sections.ts
- [ ] T029 Input validation middleware for API requests in docs/src/middleware/validation.ts
- [ ] T030 Error handling and structured logging in docs/src/middleware/errors.ts

## Phase 3.5: Integration & Validation
- [ ] T031 Markdown link validation and cross-reference checking in src/lib/docs-validator/src/validators/links.ts
- [ ] T032 Documentation completeness validation against templates in src/lib/docs-validator/src/validators/completeness.ts
- [ ] T033 Integration test: full documentation build pipeline in docs/tests/integration/test_build_pipeline.test.ts
- [ ] T034 Integration test: search functionality with real content in docs/tests/integration/test_search_integration.test.ts
- [ ] T035 Integration test: versioned documentation support in docs/tests/integration/test_versioning.test.ts

## Phase 3.6: Content & Templates
- [ ] T036 [P] Create Architecture Decision Record (ADR) template in docs/templates/adr-template.md
- [ ] T037 [P] Create API documentation template in docs/templates/api-template.md
- [ ] T038 [P] Create feature documentation template in docs/templates/feature-template.md
- [ ] T039 Migration script for existing documentation content in scripts/migrate-docs.ts

## Dependencies
**Critical Path**: T001-T008 → T009-T016 → T017-T024 → T025-T030 → T031-T035 → T036-T039

**Parallel Groups**:
- Setup: T003, T004, T005, T007, T008 (different files)
- Contract Tests: T009-T016 (different test files)
- Library Development: T017-T024 (different library files)
- Templates: T036-T038 (different template files)

**Sequential Dependencies**:
- T001 blocks T002 (directory must exist)
- T002 blocks T006 (Node.js project must be initialized)
- T009-T016 block T017-T024 (tests must fail first)
- T017-T024 block T025-T030 (libraries needed for API)
- T025-T030 block T031-T035 (API needed for integration tests)

## Parallel Example
```bash
# After T001-T008 complete, launch contract tests together:
Task: "Contract test GET /api/docs/pages in docs/tests/contract/test_pages_get.test.ts"
Task: "Contract test POST /api/docs/pages in docs/tests/contract/test_pages_post.test.ts"
Task: "Contract test GET /api/docs/search in docs/tests/contract/test_search.test.ts"
Task: "Contract test docs-build CLI in docs/tests/contract/test_cli_build.test.ts"

# After tests fail, launch library development in parallel:
Task: "DocumentationPage schema in src/lib/docs-validator/src/schemas/page.ts"
Task: "docs-build CLI in src/lib/docs-generator/src/cli/build.ts"
Task: "docs-validate CLI in src/lib/docs-validator/src/cli/validate.ts"
```

## Task Generation Rules Applied

1. **From Contracts**:
   - documentation-api.yaml → T009-T012, T025-T028 (API contract tests + endpoints)
   - cli-contracts.md → T013-T016, T021-T024 (CLI contract tests + implementations)

2. **From Data Model**:
   - DocumentationPage → T017 (schema validation)
   - DocumentationSection → T018 (schema validation)
   - SearchIndex → T019 (schema validation)
   - DocumentationTemplate → T020, T036-T038 (processing + templates)

3. **From User Stories (spec.md)**:
   - "New developer accessing documentation" → T033, T034 (integration tests)
   - "Documentation versioning" → T035 (versioning integration test)
   - "Template-based documentation" → T032, T036-T038 (validation + templates)

4. **From Research Decisions**:
   - Docusaurus 2 selection → T001, T002 (setup tasks)
   - Algolia DocSearch → T007 (search configuration)
   - Deployment strategy → T004 (CI/CD workflow)

## Validation Checklist
*GATE: Checked before execution*

- [x] All contracts have corresponding tests (T009-T016)
- [x] All entities have schema/model tasks (T017-T020)
- [x] All tests come before implementation (TDD order enforced)
- [x] Parallel tasks truly independent (different files/libraries)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] CLI libraries follow constitutional requirements
- [x] Library-first architecture maintained
- [x] Documentation content follows after infrastructure

## Notes
- [P] tasks can be executed in parallel due to file independence
- All tests must fail before implementation begins (constitutional TDD requirement)
- Commit after each completed task for progress tracking
- Documentation API is optional - can be implemented as static generation only
- Focus on constitutional compliance: libraries first, TDD enforced, CLI interfaces required