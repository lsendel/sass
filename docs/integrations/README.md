# Platform Integrations

This directory contains setup guides and documentation for integrating external services with the SASS platform.

## Available Integrations

### 🔔 Slack Notifications

Get real-time CI/CD notifications in your Slack workspace.

- **Quick Start**: Run `./scripts/setup-slack-integration.sh`
- **Full Guide**: [Slack Setup Documentation](./slack-setup.md)
- **Required Secrets**: `SLACK_BOT_TOKEN`
- **Features**:
  - Deployment notifications (staging/production)
  - Performance regression alerts
  - Security scan results
  - Custom message templates

### 🔐 GitHub Secrets Management

Securely manage credentials and API tokens for CI/CD workflows.

- **Guide**: [GitHub Secrets Management](./github-secrets-management.md)
- **Quick Reference**: Common commands and best practices
- **Topics Covered**:
  - Setting and rotating secrets
  - Environment-specific secrets
  - Security best practices
  - Troubleshooting

## Quick Setup Commands

### Slack Integration

```bash
# Automated setup (recommended)
./scripts/setup-slack-integration.sh

# Manual setup
gh secret set SLACK_BOT_TOKEN
# Then update channel IDs in workflow files
```

### AWS Deployment

```bash
# Staging credentials
gh secret set AWS_ACCESS_KEY_ID
gh secret set AWS_SECRET_ACCESS_KEY

# Production credentials
gh secret set AWS_ACCESS_KEY_ID_PROD
gh secret set AWS_SECRET_ACCESS_KEY_PROD
```

### Verify Setup

```bash
# List all configured secrets
gh secret list

# Test workflow execution
gh workflow run performance.yml
gh workflow run cd-deploy.yml
```

## Integration Status

| Integration | Status | Required | Documentation |
|------------|--------|----------|---------------|
| Slack Notifications | ✅ Configured | Optional | [slack-setup.md](./slack-setup.md) |
| AWS Deployment | ⚠️ Setup Required | Optional | Coming soon |
| StatusPage.io | ⚠️ Setup Required | Optional | Coming soon |

## Common Tasks

### Add a New Integration

1. Create setup documentation in `docs/integrations/`
2. Add secrets to GitHub: `gh secret set SECRET_NAME`
3. Update workflow files to use secrets
4. Create setup script in `scripts/` (optional)
5. Update this README with integration status

### Test Integration

```bash
# Trigger workflow manually
gh workflow run WORKFLOW_NAME

# Monitor workflow execution
gh run watch

# View recent runs
gh run list --limit 5
```

### Troubleshooting

Common issues and solutions:

| Issue | Solution |
|-------|----------|
| Secret not found | Verify with `gh secret list` |
| Invalid token | Check token format and expiration |
| Workflow fails | Review logs: `gh run view --log-failed` |
| Channel not found | Verify channel ID and bot permissions |

## Security Guidelines

🔒 **Best Practices**:
- ✅ Use environment-specific secrets for staging/production
- ✅ Rotate credentials quarterly
- ✅ Use least-privilege service accounts
- ✅ Audit secret access regularly
- ❌ Never commit secrets to git
- ❌ Never share secrets between environments
- ❌ Never use personal credentials

## Support

- **Setup Issues**: See integration-specific documentation
- **Security Concerns**: Contact security team immediately
- **General Questions**: Create an issue in the repository

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Secrets Guide](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Slack API Documentation](https://api.slack.com/docs)
- [AWS IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
