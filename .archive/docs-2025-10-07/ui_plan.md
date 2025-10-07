# UI Plan: LLM-Generated Code — Linting, UI Generation, and Validation

This plan defines how we will generate, validate, and maintain UI code when using LLMs (Claude, Codex, etc.) in this project. It tailors industry best practices to our stack and adds critiques and improvements to the provided guidance.

Project snapshot

- Frontend: Vite + React + TypeScript + Tailwind + ESLint + Prettier + Vitest + Playwright
- Backend: Spring Boot (Gradle) with Checkstyle
- Current repo state: ESLint, Prettier, Playwright, Tailwind, and Vitest already exist in `frontend/`
- Focus: This plan primarily covers the frontend; it includes cross‑cutting quality and governance steps touching CI and documentation

Goals

- Make AI‑generated UI code safe, consistent, and maintainable
- Catch issues early via layered validation (lint → typecheck → unit → interaction → e2e → visual → a11y)
- Keep velocity high without sacrificing reliability, security, or accessibility

Non‑goals

- Replacing human review; this formalizes when and how to review AI‑generated UI
- Rewriting existing app architecture; scope is quality gates, workflow, and incremental UI improvements

---

## 1) Critique of the Provided Best Practices

What’s strong

- Emphasizes layered validation (static + dynamic) and CI automation
- Advocates Storybook, interaction tests, visual regression, and accessibility testing
- Recommends multi‑stage validation including self‑review and cross‑model review

What needs refinement or caution

- Evidence quality: Several sources are blogs, marketing, or Reddit. Treat specific statistics (e.g., “true positive rates”) as directional, not definitive; rely on our own metrics.
- Prettier vs ESLint ordering: The doc says “ensure Prettier runs before ESLint.” In practice, the safest approach is:
  - Use `eslint-config-prettier` to disable styling conflicts
  - Run `eslint --max-warnings=0` for correctness and style‑enforced rules
  - Run Prettier as a separate formatter (or via ESLint plugin) in CI; exact order is less important than eliminating conflicts and failing CI on drift
- Specialized tools (Vibelint, CodeRabbit): Useful but optional. Start with foundational linters, type checks, tests, and Storybook; add AI‑specific tools after core stability is proven.
- Visual testing pitfalls: Flaky diffs from anti‑aliasing, animations, fonts. Require deterministic rendering (seeded data, mock time, disable animations) and baseline review workflow.
- Security and privacy: Visual services (e.g., hosted snapshot platforms) may handle sensitive data. Enforce data redaction and private projects; consider self‑hosted snapshots if necessary.
- Design system alignment: The doc states the need but not the concrete enforcement. We should add lint rules, checklists, and Storybook docs to guardrail component usage.
- TypeScript rigor: Strengthen type safety (e.g., `strict`, `noUncheckedIndexedAccess`) to reduce AI‑introduced edge cases.

Additional suggestions beyond the doc

- Tailwind discipline: Add class sorting and content scanning checks to control class bloat and ensure purging; prefer design tokens via CSS vars layered with Tailwind utilities.
- Import hygiene: Enforce `import/order`, ban default exports in shared libs, and prefer explicit exports to aid refactors and LLM context.
- Testing-library patterns: Add ESLint Testing Library plugin and React hooks/JSX a11y rules to prevent common UI testing mistakes.
- Golden path “Component Cookbook”: Provide ready‑made patterns (loading, error, empty, skeleton, responsive, a11y) for LLMs and humans; stories act as executable docs.
- Diff‑driven AI changes: Require LLMs to provide unified diffs, rationale, and risk notes in PRs; tag commits authored by LLMs.
- Observability: Frontend error and performance telemetry (e.g., Sentry, Web Vitals) to catch regressions missed by tests.
- i18n and RTL: Define standards now to avoid costly retrofits (copy length, direction, locale‑aware formatting).
- Performance budgets: Enforce bundle size and route‑level budgets to curb incremental bloat from AI additions.

---

## 2) Architecture and Conventions (Frontend)

Standards

- Component structure: Co‑locate component, styles, tests, and stories: `src/components/<Feature>/<Component>/`
- State management: Prefer local state and hooks; avoid global state unless necessary. Encapsulate API surface via typed clients and selectors.
- Props design: Use narrow, documented props; favor discriminated unions for variant behavior; avoid `any` and implicit `undefined`.
- Styles: Tailwind for layout and utility; design tokens via CSS variables for brand/theming. Avoid ad‑hoc inline styles except for dynamic values.
- Accessibility: Keyboard‑first navigation, proper roles/labels, focus management, color contrast, prefers‑reduced‑motion support.

Acceptance checklist for new UI

- [ ] Storybook stories for default/hover/active/disabled/error/loading/empty/responsive
- [ ] Unit + interaction tests (Testing Library + user-event)
- [ ] A11y checks (axe) pass; no critical violations
- [ ] Visual baseline approved (stable snapshots)
- [ ] TypeScript strict; no `any`/`unknown` leaks to public API
- [ ] Performance: no large bundle deltas; images optimized

---

## 3) Linting, Formatting, and Type Safety

Targets (use existing config as baseline in `frontend/`)

- ESLint
  - Add/enforce: `@typescript-eslint`, `eslint-plugin-import`, `eslint-plugin-react-hooks`, `eslint-plugin-jsx-a11y`, `eslint-plugin-testing-library`
  - Key rules: `no-unused-vars`, `no-console` (warn), `prefer-const`, `no-floating-promises`, `import/order`, `react-hooks/rules-of-hooks`, `jsx-a11y/*`
  - Use `eslint-config-prettier` to avoid conflicts
- Prettier
  - Keep opinionated defaults; fail CI on drift (`--check`)
- TypeScript
  - Enable/confirm: `strict: true`, `noUncheckedIndexedAccess`, `noImplicitOverride`, `exactOptionalPropertyTypes`
  - Add `tsc --noEmit` to CI to catch type regressions
- Tailwind
  - Add class sorting plugin; ensure purge content globs are correct to minimize CSS size
- Git Hygiene
  - Pre-commit: `lint-staged` for ESLint + Prettier
  - Pre-push: unit tests and typecheck on changed packages

Example ESLint + Prettier intent (do not change code yet)

```json
// ESLint extends: ["eslint:recommended", "plugin:@typescript-eslint/recommended", "plugin:prettier/recommended"]
// Prettier: { "singleQuote": true, "semi": true, "trailingComma": "es5", "printWidth": 100, "tabWidth": 2 }
```

---

## 4) Storybook and Component-Driven Development

Objectives

- Build components in isolation; stories serve as living specs and LLM anchors
- Interaction tests capture behavioral contracts; docs mode improves discovery and reuse

Plan

- Add Storybook (Vite builder), a11y, interactions, and docs addons
- Author stories for all shared components and new AI‑generated components with states and edge cases
- Add Storybook interaction tests (play function) for common flows
- Optional: route Playwright to Storybook for deterministic visual snapshots

Acceptance

- [ ] Storybook builds in CI (static export)
- [ ] All shared components have stories with at least: default, hover, focus, disabled, error, loading, empty, responsive
- [ ] Docs pages include props tables, a11y notes, and usage examples

---

## 5) Testing Strategy

Unit and interaction tests (Vitest + RTL)

- Use Testing Library to assert behavior, not implementation details
- Property‑based tests for data‑heavy utilities (fast‑check)

E2E and cross‑browser (Playwright)

- Deterministic tests: mock time, network, and random seeds
- Cover Chromium, Firefox, WebKit; include mobile viewport for key screens
- Use Playwright trace viewer on failures; screenshots only on failure

Accessibility testing

- Integrate `axe-core` with component and E2E tests
- Fail builds on critical violations; log minors for triage

Mutation testing (optional, staged)

- Introduce Stryker for critical UI utilities once base coverage is stable

Acceptance

- [ ] `vitest run` passes with meaningful coverage
- [ ] Playwright suite green across all projects (desktop/mobile)
- [ ] No critical a11y violations in CI

---

## 6) Visual Regression Testing

Approach

- Prefer deterministic snapshots: fixed fonts, disabled animations, mocked data
- Baselines stored per branch with review flow for updates
- Start small (critical components + key screens), expand by risk/impact

Acceptance

- [ ] Baselines stable (<2% churn per week)
- [ ] All updates reviewed with diff context and changelog notes

---

## 7) Design System Alignment

Strategy

- Define tokens (spacing, color, radii, typography) as CSS variables layered under Tailwind
- Prefer headless patterns plus utility classes; avoid custom ad‑hoc patterns
- Create a “Component Cookbook” in Storybook documenting recipes and anti‑patterns

Enforcement

- Lint rules for forbidden raw colors and font sizes outside tokens
- PR checklist item for design compliance

Acceptance

- [ ] Tokens documented and used in new components
- [ ] Cookbook stories cover frequent patterns (forms, modals, tables, toasts)

---

## 8) CI/CD Quality Gates (GitHub Actions)

Jobs (sequence)

- Install: `npm ci`
- Format: `prettier --check`
- Lint: `eslint . --max-warnings=0`
- Typecheck: `tsc --noEmit`
- Unit: `vitest run`
- Build Storybook
- Visual tests (deterministic snapshots)
- E2E (selected smoke on PR; full on main/nightly)

Branch protection

- Require all quality checks to pass
- At least one human reviewer for AI‑generated PRs; block direct merges for `ai-gen` labeled PRs without review

Artifacts and caching

- Cache node_modules based on lockfile hash
- Upload Playwright traces on failures; store Storybook static build for review

---

## 9) Governance for AI‑Generated Changes

Commit policy

- Tag AI‑generated commits with `[ai-gen]` and include model, temperature, and prompt summary in the PR body
- Require PR description to include: scope, risks, testing evidence, and a diff of changes the model intended

Prompt templates (use and adapt as needed)

Generation prompt (components)

```
You are contributing a React + TypeScript component to a Vite + Tailwind project.
Constraints:
- Follow existing folder conventions and design tokens; Tailwind utilities only, no inline styling except dynamic values.
- Implement loading, error, empty, and responsive states.
- Ensure accessibility: keyboard support, roles/labels, focus ring, color contrast.
- Write minimal, composable logic; no global state unless necessary.
- Provide Storybook stories for all states and a short MDX doc.
- Provide Vitest + Testing Library tests for interactions.
- Avoid external deps unless approved; keep bundle small.
Return: a unified diff with file paths relative to `frontend/`, rationale, and risks.
```

Self‑review prompt (LLM)

```
Review the following diff for bugs, a11y, type safety, and performance risks. Propose minimal changes to fix issues. Flag risky patterns (any, unsafe casts, unhandled promises, missing aria, hard‑coded colors, large imports).
```

Cross‑model validation prompt

```
Independently review the diff for logic and design-system compliance. Do not restate changes; identify gaps and contradictions. Output a checklist to verify with tests and stories.
```

Reviewer aid prompt

```
Given this component and its stories, generate test ideas for edge cases, failure modes, and a11y pitfalls. Prioritize by risk.
```

---

## 10) Security, Privacy, and Compliance (Frontend)

Security checks

- Lint for `dangerouslySetInnerHTML`, unescaped HTML, and insecure URL handling
- Input validation at boundaries; prefer schema validation for external data
- CSP and dependency audits in CI; pin dependencies via lockfile

Privacy

- Redact PII in visual snapshots and logs; mock data in tests
- Review external SaaS usage for data handling terms

Acceptance

- [ ] No blocked security lint rules
- [ ] Dependency audits pass; no critical advisories

---

## 11) Performance and Bundle Health

Budgets and analysis

- Set bundle budgets per route/component; fail PRs exceeding thresholds
- Use Vite plugin visualizer to track bundle composition over time
- Encourage code‑splitting and lazy loading for heavy UI regions

Runtime performance

- Measure Web Vitals; monitor regressions alongside deployments
- Avoid expensive synchronous work on mount; prefer suspense and progressive hydration patterns where applicable

Acceptance

- [ ] Bundle budgets enforced in CI
- [ ] No significant regressions in Web Vitals after merges

---

## 12) Documentation and Knowledge

Artifacts

- This `ui_plan.md` serves as the living source of truth
- Add an `AGENTS.md` to guide coding agents on project conventions and guardrails (paths, naming, tokens, test types)
- Keep `README` updated with scripts and quality gate explanations

Onboarding

- Provide a short “LLM usage guide” with do/don’t examples and prompt templates
- Record a short walkthrough video of the Storybook + testing workflow

---

## 13) Phased Timeline and Ownership

Phase 0 (Audit) — 0.5 day

- Verify ESLint/Prettier/TS config alignment, Tailwind purge, Playwright stability

Phase 1 (Quality gates) — 1–2 days

- Harden ESLint rules, Prettier checks, TS strict; add lint‑staged and CI checks

Phase 2 (Storybook) — 1–2 days

- Install, configure addons, add stories for core components; enable interaction tests

Phase 3 (Tests) — 2–4 days

- Strengthen Vitest suites, deterministic Playwright, add axe checks, seed data

Phase 4 (Visual + Budgets) — 1–2 days

- Introduce visual baselines; set and enforce bundle budgets

Phase 5 (Governance + Docs) — 1 day

- Add PR templates, AI commit policy, AGENTS.md, and onboarding docs

---

## 14) Success Metrics

- Pre‑merge: 100% green on format, lint, typecheck, unit, interaction, a11y; stable visual diffs
- Post‑merge: No increase in frontend error rate; Web Vitals steady or improved
- Review: Fewer rework cycles on AI‑generated components; reduced bundle growth rate

---

## 15) Immediate Next Actions (for this repo)

- [ ] Run a quick audit of `frontend/.eslintrc.cjs`, `.prettierrc`, `tsconfig.json`, and Playwright config against this plan
- [ ] Draft PR template with AI disclosure section and test evidence checklist
- [ ] Set up GitHub Actions jobs for: format check, lint, typecheck, unit, Storybook build, Playwright smoke
- [ ] Identify 3–5 core components to backfill with stories + interaction tests as exemplars
- [ ] Define initial design tokens (if not already) and document in Storybook

---

References note

- The original list mixes vendor posts, blogs, and community threads; useful for ideas but not authoritative. This plan prioritizes practices validated by our stack and metrics. We will adjust based on local CI stability, defect rates, bundle trends, and reviewer feedback.
