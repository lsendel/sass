# GitHub Environments Setup Scripts

This directory contains automation scripts for setting up and managing GitHub Environments.

## Available Scripts

### 1. `setup-environments.sh`
**Purpose:** Automatically creates GitHub Environments (staging and production)

**Usage:**
```bash
.github/scripts/setup-environments.sh
```

**What it does:**
- Creates `staging` environment linked to `develop` branch
- Creates `production` environment linked to `main` branch with 5-minute wait timer
- Configures deployment branch policies
- Provides next-step instructions for adding secrets

**Requirements:**
- GitHub CLI (`gh`) installed and authenticated
- Repository admin access

---

### 2. `verify-secrets.sh`
**Purpose:** Verifies that all required secrets and environments are configured

**Usage:**
```bash
.github/scripts/verify-secrets.sh
```

**What it checks:**
- ✅ Environment existence (staging, production)
- ✅ Repository-level secrets
- ✅ Environment-specific secrets
- ✅ Environment variables
- ⚠️  Optional secrets (with warnings)

**Output:**
- Green checkmarks (✓) for configured items
- Red X marks (✗) for missing items
- Yellow info (ℹ) for optional items
- Summary of missing configurations

**Requirements:**
- GitHub CLI (`gh`) installed and authenticated
- Repository access (read permissions sufficient)

---

## Quick Start

### First Time Setup
```bash
# 1. Make scripts executable
chmod +x .github/scripts/*.sh

# 2. Create environments
.github/scripts/setup-environments.sh

# 3. Add secrets (follow the output instructions)
gh secret set AWS_ACCESS_KEY_ID --env staging --body "YOUR_KEY"
# ... (add other secrets)

# 4. Verify everything is configured
.github/scripts/verify-secrets.sh
```

### Regular Verification
```bash
# Run verification before important deployments
.github/scripts/verify-secrets.sh
```

---

## Script Details

### setup-environments.sh

**Exit Codes:**
- `0` - Success
- `1` - GitHub CLI not installed or not authenticated

**Environment Variables:**
None required (uses authenticated `gh` session)

**Features:**
- Color-coded output for better readability
- Automatic error handling
- Idempotent (safe to run multiple times)
- Provides detailed next-step instructions

**Example Output:**
```
🚀 GitHub Environments Setup Script
====================================

✓ GitHub CLI is installed
✓ Authenticated with GitHub

Repository: your-org/sass

📦 Setting up Staging Environment
----------------------------------
✓ Environment 'staging' created
  Setting deployment branch policy...
  Adding branch pattern: develop

📦 Setting up Production Environment
-------------------------------------
✓ Environment 'production' created
  Setting deployment branch policy...
  Adding branch pattern: main
  Setting wait timer: 5 minutes

✅ Environments created successfully!
```

---

### verify-secrets.sh

**Exit Codes:**
- `0` - All required secrets configured (may have warnings for optional)
- Non-zero exit code indicates configuration issues

**Environment Variables:**
None required (uses authenticated `gh` session)

**Features:**
- Comprehensive verification of all secret types
- Distinguishes between required and optional secrets
- Color-coded output with emoji indicators
- Detailed summary of missing configurations
- Does not reveal secret values (security by design)

**Example Output:**
```
🔍 GitHub Secrets Verification Script
======================================

✓ GitHub CLI is installed
✓ Authenticated with GitHub

📦 Checking Environments
------------------------
✓ Environment 'staging' exists
✓ Environment 'production' exists

🔐 Checking Repository Secrets (Required)
------------------------------------------
✓ AWS_REGION
✓ SONAR_TOKEN
✗ DOCKER_USERNAME
✗ DOCKER_PASSWORD
✓ SLACK_BOT_TOKEN

📋 Summary
==========
❌ 2 required repository secret(s) missing
✓ All staging environment secrets configured
✓ All production environment secrets configured
```

---

## Troubleshooting

### "gh: command not found"
Install GitHub CLI: https://cli.github.com/

**macOS:**
```bash
brew install gh
```

**Linux:**
```bash
# Debian/Ubuntu
sudo apt install gh

# Red Hat/CentOS
sudo yum install gh
```

**Windows:**
```bash
winget install GitHub.cli
```

### "Error: Not authenticated"
Authenticate with GitHub:
```bash
gh auth login
```

Follow the prompts to authenticate via browser or token.

### "Error: Forbidden"
Ensure you have admin access to the repository for setup, or at least read access for verification.

### Scripts show "Permission denied"
Make scripts executable:
```bash
chmod +x .github/scripts/*.sh
```

---

## Security Considerations

### What These Scripts Do:
- ✅ Create and configure environments
- ✅ Check if secrets exist (without revealing values)
- ✅ Provide configuration guidance

### What These Scripts Don't Do:
- ❌ Never display secret values
- ❌ Never log sensitive information
- ❌ Never send data to external services
- ❌ Never modify existing secrets

### Best Practices:
1. Run scripts from a secure terminal
2. Don't pipe output to untrusted locations
3. Review script contents before execution
4. Keep GitHub CLI updated
5. Use personal access tokens with minimal required scopes

---

## Contributing

### Adding New Secrets to Verify

Edit `verify-secrets.sh` and add to the appropriate array:

```bash
# For repository-level required secrets
REPO_SECRETS+=(
    "NEW_SECRET_NAME"
)

# For optional secrets
OPTIONAL_SECRETS+=(
    "NEW_OPTIONAL_SECRET"
)

# For environment-specific secrets
STAGING_SECRETS+=(
    "NEW_STAGING_SECRET"
)
```

### Improving Scripts

1. Test changes thoroughly
2. Maintain backward compatibility
3. Add comments for complex logic
4. Update this README
5. Keep color-coding consistent

---

## Related Documentation

- **Quick Start Guide:** `.github/QUICK-START.md`
- **Full Setup Guide:** `.github/ENVIRONMENTS.md`
- **Setup Checklist:** `.github/SETUP-CHECKLIST.md`
- **GitHub Actions Docs:** https://docs.github.com/en/actions

---

## Support

If you encounter issues:
1. Check script output for specific error messages
2. Verify GitHub CLI authentication: `gh auth status`
3. Review repository permissions
4. Consult the troubleshooting section above
5. Check GitHub Actions documentation

---

**Last Updated:** 2025-01-10
**Maintainer:** DevOps Team
