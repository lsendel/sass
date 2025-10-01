# GitHub Environments Setup

This document explains how to configure GitHub Environments to resolve the validation errors in `cd-deploy.yml`.

## Required Environments

The deployment workflow requires two environments to be configured in your GitHub repository:

### 1. Staging Environment
- **Name**: `staging`
- **Protection Rules**: Optional (recommended: require approval from 1 reviewer)
- **Deployment Branches**: `develop` branch only

### 2. Production Environment
- **Name**: `production`
- **Protection Rules**: Recommended (require approval from 2+ reviewers)
- **Deployment Branches**: `main` branch only

## Setup Instructions

### Via GitHub Web UI

1. Go to your repository on GitHub
2. Click on **Settings** tab
3. In the left sidebar, click **Environments**
4. Click **New environment**
5. Create the `staging` environment:
   - Enter name: `staging`
   - Click **Configure environment**
   - Optionally add protection rules
   - Under "Deployment branches", select "Selected branches" and add `develop`
   - Add required secrets (see below)
   - Click **Save protection rules**

6. Repeat for `production` environment:
   - Enter name: `production`
   - Click **Configure environment**
   - Add protection rules (recommended: required reviewers)
   - Under "Deployment branches", select "Selected branches" and add `main`
   - Add required secrets (see below)
   - Click **Save protection rules**

### Required Environment Secrets

#### Staging Environment Secrets
- `AWS_ACCESS_KEY_ID` - AWS access key for staging
- `AWS_SECRET_ACCESS_KEY` - AWS secret key for staging
- `EKS_CLUSTER_NAME_STAGING` - Name of staging EKS cluster

#### Production Environment Secrets
- `AWS_ACCESS_KEY_ID_PROD` - AWS access key for production
- `AWS_SECRET_ACCESS_KEY_PROD` - AWS secret key for production
- `EKS_CLUSTER_NAME_PROD` - Name of production EKS cluster

### Repository-Level Secrets

These secrets should be added at the repository level (Settings → Secrets and variables → Actions):

- `AWS_REGION` - AWS region (e.g., `us-east-1`)
- `SLACK_BOT_TOKEN` - Slack bot token for notifications
- `STATUSPAGE_PAGE_ID` - Statuspage.io page ID
- `STATUSPAGE_COMPONENT_ID` - Statuspage.io component ID
- `STATUSPAGE_API_KEY` - Statuspage.io API key
- `SONAR_TOKEN` - SonarCloud token
- `SNYK_TOKEN` - Snyk API token (optional)
- `GITGUARDIAN_API_KEY` - GitGuardian API key (optional)
- `DOCKER_USERNAME` - Docker Hub username
- `DOCKER_PASSWORD` - Docker Hub password/token

## Alternative: Skip Environment Validation in IDE

If you don't want to set up environments immediately, you can suppress the IDE warnings:

### VS Code
Add to `.vscode/settings.json`:
```json
{
  "yaml.customTags": [
    "!environment scalar"
  ]
}
```

### IntelliJ IDEA
The warnings can be safely ignored - they won't prevent the workflows from running.

## Verifying Configuration

After setting up the environments, the workflow will:
1. Wait for required approvals (if configured)
2. Only run on the specified branches
3. Use environment-specific secrets
4. Show deployment status in GitHub UI

## Testing

To test the environment setup:
1. Make a change and push to `develop` branch
2. Wait for CI to pass
3. Check the Actions tab - you should see the staging deployment waiting for approval (if configured)
4. Approve the deployment
5. Verify staging deployment completes successfully
