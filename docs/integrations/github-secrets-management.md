# GitHub Secrets Management Guide

This guide covers how to manage GitHub Actions secrets for the SASS platform, including Slack integration, cloud credentials, and other sensitive data.

## Overview

GitHub Actions secrets are encrypted environment variables used in CI/CD workflows. They should be used for:
- API tokens and credentials
- Cloud provider access keys
- Third-party service tokens
- Signing keys and certificates

## Quick Setup

### Using GitHub CLI (Recommended)

```bash
# Set a secret interactively
gh secret set SECRET_NAME

# Set a secret from a file
gh secret set SECRET_NAME < secret-file.txt

# Set a secret from command output
echo "my-secret-value" | gh secret set SECRET_NAME

# List all secrets
gh secret list

# Delete a secret
gh secret delete SECRET_NAME
```

### Using GitHub Web UI

1. Navigate to repository: `https://github.com/lsendel/sass`
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"**
4. Enter name and value
5. Click **"Add secret"**

## Required Secrets for SASS Platform

### Slack Integration

| Secret Name | Description | Required | Example |
|-------------|-------------|----------|---------|
| `SLACK_BOT_TOKEN` | Slack Bot User OAuth Token | Optional | `xoxb-1234567890-...` |

**Setup**: Run `./scripts/setup-slack-integration.sh` or follow [Slack Setup Guide](./slack-setup.md)

### AWS Deployment (Optional)

| Secret Name | Description | Required | Example |
|-------------|-------------|----------|---------|
| `AWS_ACCESS_KEY_ID` | AWS access key for staging | Only for staging | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key for staging | Only for staging | `wJalrXUtnFEMI/K7MDENG/...` |
| `AWS_ACCESS_KEY_ID_PROD` | AWS access key for production | Only for production | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY_PROD` | AWS secret key for production | Only for production | `wJalrXUtnFEMI/K7MDENG/...` |

**Setup**: See [AWS Deployment Guide](../deployment/aws-setup.md)

### StatusPage.io (Optional)

| Secret Name | Description | Required | Example |
|-------------|-------------|----------|---------|
| `STATUSPAGE_API_KEY` | StatusPage.io API key | Optional | `abc123...` |
| `STATUSPAGE_PAGE_ID` | StatusPage.io page ID | Optional | `xyz789...` |
| `STATUSPAGE_COMPONENT_ID` | Component ID to update | Optional | `comp456...` |

### Code Signing (Optional)

| Secret Name | Description | Required | Example |
|-------------|-------------|----------|---------|
| `GPG_PRIVATE_KEY` | GPG private key for signing | Optional | `-----BEGIN PGP...` |
| `GPG_PASSPHRASE` | GPG key passphrase | Optional | `secure-passphrase` |

## Environment-Specific Secrets

GitHub supports environment-specific secrets for staging/production isolation.

### Creating Environment Secrets

```bash
# Create an environment first (via GitHub UI)
# Then set environment-specific secrets

# Using gh CLI with environment
gh secret set SECRET_NAME --env production
gh secret set SECRET_NAME --env staging

# List secrets for an environment
gh secret list --env production
```

### Via GitHub UI

1. **Settings** → **Environments**
2. Click environment name (e.g., `production`)
3. Click **"Add secret"**
4. Enter name and value

### Recommended Environment Structure

```
environments/
├── staging/
│   ├── AWS_ACCESS_KEY_ID
│   ├── AWS_SECRET_ACCESS_KEY
│   └── DATABASE_URL
└── production/
    ├── AWS_ACCESS_KEY_ID_PROD
    ├── AWS_SECRET_ACCESS_KEY_PROD
    └── DATABASE_URL_PROD
```

## Security Best Practices

### 1. Principle of Least Privilege

✅ **Do:**
- Create service accounts with minimal permissions
- Use environment-specific credentials
- Rotate secrets regularly

❌ **Don't:**
- Use personal credentials
- Give admin access unless required
- Share secrets across environments

### 2. Secret Rotation

```bash
# Rotate Slack token
./scripts/rotate-slack-token.sh

# Rotate AWS credentials
./scripts/rotate-aws-credentials.sh

# Schedule regular rotations (quarterly recommended)
```

### 3. Audit Secret Access

```bash
# View secret usage in workflows
gh secret list

# Check workflow runs
gh run list --workflow=cd-deploy.yml

# Review GitHub audit log
# Settings → Security → Audit log
```

### 4. Never Commit Secrets

```bash
# Add to .gitignore
echo ".env" >> .gitignore
echo "*.pem" >> .gitignore
echo "secrets/" >> .gitignore

# Use git-secrets to prevent commits
git secrets --install
git secrets --register-aws
```

## Working with Secrets in Workflows

### Accessing Secrets

```yaml
steps:
  - name: Deploy with secrets
    run: ./deploy.sh
    env:
      SLACK_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}
      AWS_KEY: ${{ secrets.AWS_ACCESS_KEY_ID }}
```

### Conditional Secrets

```yaml
# Only use secret if it exists
- name: Notify Slack
  if: ${{ secrets.SLACK_BOT_TOKEN != '' }}
  uses: slackapi/slack-github-action@v1.24.0
  env:
    SLACK_BOT_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}
```

### Masking Secret Output

```yaml
# Secrets are automatically masked in logs
- name: Echo secret (will be masked)
  run: echo "Token: ${{ secrets.SLACK_BOT_TOKEN }}"
  # Output: Token: ***
```

### Multi-line Secrets

```bash
# Set multi-line secret (e.g., private key)
gh secret set GPG_PRIVATE_KEY < private-key.pem

# Or interactively
gh secret set GPG_PRIVATE_KEY
# Paste content, then Ctrl+D to finish
```

## Common Workflows

### Setting Up All Required Secrets

```bash
#!/bin/bash
# setup-all-secrets.sh

# Slack Integration
./scripts/setup-slack-integration.sh

# AWS Credentials (if deploying to AWS)
gh secret set AWS_ACCESS_KEY_ID
gh secret set AWS_SECRET_ACCESS_KEY
gh secret set AWS_ACCESS_KEY_ID_PROD
gh secret set AWS_SECRET_ACCESS_KEY_PROD

# Verify all secrets
gh secret list
```

### Copying Secrets Between Repositories

```bash
# Export from source repo
cd /path/to/source-repo
gh secret list > secrets-list.txt

# Set in target repo
cd /path/to/target-repo
gh secret set SECRET_NAME  # Manually enter values
```

**Note**: Secrets cannot be read once set, only updated/deleted.

### Environment Variable Mapping

```yaml
# .github/workflows/example.yml
env:
  # Repository-level secrets
  SLACK_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}

  # Environment-level secrets
  AWS_KEY: ${{ secrets.AWS_ACCESS_KEY_ID }}

  # Variables (non-sensitive)
  AWS_REGION: ${{ vars.AWS_REGION }}
  EKS_CLUSTER: ${{ vars.EKS_CLUSTER_NAME_PROD }}
```

## Troubleshooting

### Secret Not Available in Workflow

**Issue**: `${{ secrets.SECRET_NAME }}` is empty

**Solutions**:
1. Verify secret exists: `gh secret list`
2. Check secret name matches exactly (case-sensitive)
3. Ensure workflow has access to environment
4. For pull requests from forks, secrets aren't available

### "Invalid Format" Error

**Issue**: Secret value is rejected

**Solutions**:
```bash
# Remove trailing newlines
echo -n "value" | gh secret set SECRET_NAME

# Base64 encode binary data
base64 secret-file.bin | gh secret set SECRET_NAME

# For JSON, ensure proper escaping
gh secret set JSON_SECRET < config.json
```

### Secret Rotation Failed

**Issue**: Old secret still in use after rotation

**Solutions**:
1. Update secret in GitHub first
2. Trigger workflow manually to test
3. Monitor workflow logs for errors
4. Rollback if necessary: restore old value

### Workflow Fails with "Invalid Credentials"

**Issue**: Authentication fails despite correct secret

**Solutions**:
1. Verify secret value has no extra whitespace
2. Check token hasn't expired or been revoked
3. Verify permissions/scopes are sufficient
4. Test credential outside GitHub Actions first

## Monitoring and Alerts

### Track Secret Usage

```bash
# View recent workflow runs
gh run list --limit 10

# Check specific workflow run
gh run view RUN_ID --log

# Monitor for failures
gh run list --status failure
```

### Audit Secret Changes

Via GitHub UI:
1. **Settings** → **Security** → **Audit log**
2. Filter by: `action:repo.add_secret` or `action:repo.remove_secret`
3. Export for compliance records

### Set Up Alerts

```yaml
# .github/workflows/security-monitoring.yml
name: Security Monitoring

on:
  schedule:
    - cron: '0 0 * * *'  # Daily

jobs:
  check-secrets:
    runs-on: ubuntu-latest
    steps:
      - name: Verify critical secrets exist
        run: |
          # Check that required secrets are set
          if [ -z "${{ secrets.SLACK_BOT_TOKEN }}" ]; then
            echo "⚠️  SLACK_BOT_TOKEN is missing"
          fi
```

## Migration and Backup

### Backup Strategy

**⚠️ Important**: Secrets cannot be exported. Maintain secure backup separately.

1. Document all secrets in a secrets inventory
2. Store values in team password manager (1Password, Vault, etc.)
3. Keep recovery procedures documented
4. Test secret restoration quarterly

### Secrets Inventory Template

```markdown
# Secrets Inventory - SASS Platform

Last Updated: 2025-10-02

| Secret Name | Purpose | Owner | Rotation Schedule | Backup Location |
|-------------|---------|-------|-------------------|-----------------|
| SLACK_BOT_TOKEN | CI/CD notifications | DevOps | Quarterly | 1Password |
| AWS_ACCESS_KEY_ID | Staging deployment | DevOps | Monthly | AWS Secrets Manager |
```

## Related Documentation

- [Slack Setup Guide](./slack-setup.md)
- [AWS Deployment Guide](../deployment/aws-setup.md)
- [GitHub Actions Security](https://docs.github.com/en/actions/security-guides)
- [Encrypted Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

## Quick Reference

### Common Commands

```bash
# Set secret
gh secret set SECRET_NAME

# List secrets
gh secret list

# Delete secret
gh secret delete SECRET_NAME

# Set from file
gh secret set SECRET_NAME < file.txt

# Set for specific environment
gh secret set SECRET_NAME --env production

# View secret usage
gh api repos/:owner/:repo/actions/secrets
```

### Workflow Syntax

```yaml
# Access secret
${{ secrets.SECRET_NAME }}

# Check if exists
if: ${{ secrets.SECRET_NAME != '' }}

# Use in env
env:
  TOKEN: ${{ secrets.SECRET_NAME }}
```

## Support

For assistance:
- **Security Issues**: Contact security team immediately
- **Setup Help**: See [Slack Setup Guide](./slack-setup.md)
- **General Questions**: Create an issue in the repository
