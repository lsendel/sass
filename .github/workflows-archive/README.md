# Archived Workflows

This directory contains the old GitHub Actions workflows that have been replaced by the new optimized CI/CD pipeline.

## Archived Files

The following workflows have been replaced:

- `backend-ci.yml` → Replaced by `ci-main.yml`
- `claude-code-review.yml` → Functionality integrated into `ci-main.yml`
- `claude.yml` → Functionality integrated into `ci-main.yml`
- `docs.yml` → Functionality integrated into `ci-main.yml`
- `e2e-tests.yml` → Functionality integrated into `ci-main.yml`
- `enhanced-ci-cd.yml` → Replaced by `ci-main.yml` and `cd-deploy.yml`
- `enhanced-ci.yml` → Replaced by `ci-main.yml`
- `frontend-ci.yml` → Functionality integrated into `ci-main.yml`
- `k8s-validate.yml` → Functionality integrated into `cd-deploy.yml`
- `production-ci-cd.yml` → Replaced by `cd-deploy.yml`
- `quality-gates.yml` → Functionality integrated into `ci-main.yml`
- `security-scan.yml` → Enhanced and integrated into `ci-main.yml`
- `sonarqube.yml` → Functionality integrated into `ci-main.yml`
- `tools-ci.yml` → Functionality integrated into `ci-main.yml`

## New Workflow Structure

The new CI/CD pipeline consists of:

1. **`ci-main.yml`** - Main CI pipeline with:
   - Constitutional compliance checks
   - Security scanning
   - Backend testing
   - Code quality validation
   - Docker image building

2. **`cd-deploy.yml`** - Deployment pipeline with:
   - Staging deployment
   - Production blue-green deployment
   - Smoke tests and validation

## Safe Removal

These archived files can be safely removed after the new CI/CD pipeline has been tested and validated in production for at least 2 weeks.

## Rollback Plan

If issues arise with the new pipeline, these archived workflows can be restored by moving them back to the `.github/workflows/` directory.

---
Created: $(date)
Archive Reason: CI/CD pipeline optimization and consolidation