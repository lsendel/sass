# Quick Start: GitHub Environments Setup

**Goal:** Set up GitHub Environments to eliminate the 2 validation errors in `cd-deploy.yml`

## 🚀 Quick Setup (5 minutes)

### 1. Run Automated Setup

```bash
# Make scripts executable (if not already)
chmod +x .github/scripts/*.sh

# Create environments automatically
.github/scripts/setup-environments.sh
```

### 2. Add Minimum Required Secrets

```bash
# Repository secrets (required for workflows to run)
gh secret set AWS_REGION --body "us-east-1"
gh secret set SLACK_BOT_TOKEN --body "skip-if-not-using-slack"

# Staging environment
gh secret set AWS_ACCESS_KEY_ID --env staging --body "YOUR_STAGING_KEY"
gh secret set AWS_SECRET_ACCESS_KEY --env staging --body "YOUR_STAGING_SECRET"
gh variable set EKS_CLUSTER_NAME_STAGING --env staging --body "staging-cluster"

# Production environment
gh secret set AWS_ACCESS_KEY_ID_PROD --env production --body "YOUR_PROD_KEY"
gh secret set AWS_SECRET_ACCESS_KEY_PROD --env production --body "YOUR_PROD_SECRET"
gh variable set EKS_CLUSTER_NAME_PROD --env production --body "prod-cluster"
```

### 3. Verify Setup

```bash
.github/scripts/verify-secrets.sh
```

### 4. Reload VS Code

After creating the environments, reload VS Code to clear the validation errors:

- Press `Cmd+Shift+P` (Mac) or `Ctrl+Shift+P` (Windows/Linux)
- Type "Reload Window" and press Enter

**✅ Done!** The 2 errors in `cd-deploy.yml` should now be resolved.

---

## 📋 What Was Created?

| Environment    | Branch    | Wait Time | URL                         |
| -------------- | --------- | --------- | --------------------------- |
| **staging**    | `develop` | 0 min     | https://staging.example.com |
| **production** | `main`    | 5 min     | https://app.example.com     |

---

## 🔐 Full Secret List (Optional)

For complete functionality, add these secrets:

**Repository Level:**

```bash
gh secret set SONAR_TOKEN --body "YOUR_TOKEN"
gh secret set DOCKER_USERNAME --body "YOUR_USERNAME"
gh secret set DOCKER_PASSWORD --body "YOUR_PASSWORD"
gh secret set STATUSPAGE_PAGE_ID --body "YOUR_PAGE_ID"
gh secret set STATUSPAGE_COMPONENT_ID --body "YOUR_COMPONENT_ID"
gh secret set STATUSPAGE_API_KEY --body "YOUR_API_KEY"
gh secret set SNYK_TOKEN --body "YOUR_TOKEN"  # Optional
gh secret set GITGUARDIAN_API_KEY --body "YOUR_KEY"  # Optional
```

---

## 🎯 Next Steps

1. **Configure Protection Rules** (Recommended for Production)
   - Go to: Settings → Environments → production
   - Enable "Required reviewers"
   - Add team members who can approve deployments

2. **Test Deployment**
   - Push a change to `develop` branch
   - Watch the staging deployment run automatically
   - Merge to `main` and approve the production deployment

3. **Customize URLs**
   - Update `cd-deploy.yml` lines 25 and 128 with your actual URLs

---

## 📚 Additional Resources

- **Full Setup Guide:** `.github/ENVIRONMENTS.md`
- **Step-by-Step Checklist:** `.github/SETUP-CHECKLIST.md`
- **Verify Script:** `.github/scripts/verify-secrets.sh`

---

## ❓ Troubleshooting

**Q: Errors still showing in VS Code?**

- Reload VS Code window (Cmd+Shift+P → "Reload Window")
- Check that environments were created: `gh api repos/{owner}/{repo}/environments`

**Q: "gh: command not found"**

- Install GitHub CLI: https://cli.github.com/

**Q: Can I set up via GitHub UI instead?**

- Yes! Go to Settings → Environments → New environment
- Create `staging` and `production`
- Add secrets manually

**Q: Do I need all the secrets?**

- Minimum required: AWS credentials for both environments
- Other secrets enable specific features (SonarCloud, Slack, etc.)
- Missing optional secrets will be skipped in workflows

---

## 🆘 Need Help?

Run the verification script to see what's missing:

```bash
.github/scripts/verify-secrets.sh
```

Check the full documentation:

```bash
cat .github/ENVIRONMENTS.md
```
