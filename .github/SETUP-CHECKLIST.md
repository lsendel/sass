# GitHub Environments Setup Checklist

Use this checklist to set up your GitHub Environments step by step.

## Prerequisites

- [ ] GitHub CLI (`gh`) installed ([Install](https://cli.github.com/))
- [ ] Authenticated with GitHub (`gh auth login`)
- [ ] Repository access with admin permissions
- [ ] AWS credentials ready (for staging and production)
- [ ] Third-party service tokens ready (Slack, Statuspage, SonarCloud, etc.)

## Step 1: Create Environments (Automated)

Run the automated setup script:

```bash
.github/scripts/setup-environments.sh
```

This will create:
- ✅ `staging` environment (linked to `develop` branch)
- ✅ `production` environment (linked to `main` branch, 5-minute wait timer)

**Manual Alternative (via GitHub UI):**
1. Go to: `Settings` → `Environments`
2. Click `New environment`
3. Create `staging` and `production` environments
4. Configure deployment branch policies

## Step 2: Configure Repository-Level Secrets

Set up secrets that apply to all workflows:

```bash
# Required secrets
gh secret set AWS_REGION --body "us-east-1"
gh secret set SONAR_TOKEN --body "YOUR_SONAR_TOKEN"
gh secret set DOCKER_USERNAME --body "YOUR_DOCKER_USERNAME"
gh secret set DOCKER_PASSWORD --body "YOUR_DOCKER_TOKEN"
gh secret set SLACK_BOT_TOKEN --body "xoxb-YOUR-SLACK-TOKEN"
gh secret set STATUSPAGE_PAGE_ID --body "YOUR_PAGE_ID"
gh secret set STATUSPAGE_COMPONENT_ID --body "YOUR_COMPONENT_ID"
gh secret set STATUSPAGE_API_KEY --body "YOUR_API_KEY"

# Optional but recommended
gh secret set SNYK_TOKEN --body "YOUR_SNYK_TOKEN"
gh secret set GITGUARDIAN_API_KEY --body "YOUR_GITGUARDIAN_KEY"
```

**Checklist:**
- [ ] AWS_REGION
- [ ] SONAR_TOKEN
- [ ] DOCKER_USERNAME
- [ ] DOCKER_PASSWORD
- [ ] SLACK_BOT_TOKEN
- [ ] STATUSPAGE_PAGE_ID
- [ ] STATUSPAGE_COMPONENT_ID
- [ ] STATUSPAGE_API_KEY
- [ ] SNYK_TOKEN (optional)
- [ ] GITGUARDIAN_API_KEY (optional)

## Step 3: Configure Staging Environment

Set up secrets and variables for the staging environment:

```bash
# Staging secrets
gh secret set AWS_ACCESS_KEY_ID --env staging --body "AKIAIOSFODNN7EXAMPLE"
gh secret set AWS_SECRET_ACCESS_KEY --env staging --body "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"

# Staging variables
gh variable set EKS_CLUSTER_NAME_STAGING --env staging --body "sass-staging-cluster"
```

**Checklist:**
- [ ] AWS_ACCESS_KEY_ID (staging)
- [ ] AWS_SECRET_ACCESS_KEY (staging)
- [ ] EKS_CLUSTER_NAME_STAGING (staging variable)

## Step 4: Configure Production Environment

Set up secrets and variables for the production environment:

```bash
# Production secrets
gh secret set AWS_ACCESS_KEY_ID_PROD --env production --body "AKIAIOSFODNN7EXAMPLE"
gh secret set AWS_SECRET_ACCESS_KEY_PROD --env production --body "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"

# Production variables
gh variable set EKS_CLUSTER_NAME_PROD --env production --body "sass-production-cluster"
```

**Checklist:**
- [ ] AWS_ACCESS_KEY_ID_PROD (production)
- [ ] AWS_SECRET_ACCESS_KEY_PROD (production)
- [ ] EKS_CLUSTER_NAME_PROD (production variable)

## Step 5: Configure Protection Rules (Production)

**Via GitHub UI** (Settings → Environments → production):

1. **Required reviewers:**
   - [ ] Enable "Required reviewers"
   - [ ] Add 2-3 team members who can approve production deployments
   - [ ] Consider adding a deployment approval team

2. **Wait timer:**
   - [ ] Already set to 5 minutes (configured by script)
   - [ ] Adjust if needed for your team's workflow

3. **Deployment branches:**
   - [ ] Already configured for `main` branch only
   - [ ] Verify the setting

4. **Environment secrets (verify):**
   - [ ] Confirm all production secrets are set
   - [ ] Test secret access (secrets are hidden but show as "Updated X ago")

## Step 6: Verify Setup

Run the verification script to check all configurations:

```bash
.github/scripts/verify-secrets.sh
```

Expected output:
- ✅ All environments exist
- ✅ All required secrets configured
- ✅ All environment-specific secrets set

**Checklist:**
- [ ] Verification script runs without errors
- [ ] All secrets show as configured
- [ ] Both environments are accessible

## Step 7: Test Deployments

### Test Staging Deployment

1. Create a feature branch and make a small change
2. Merge to `develop` branch
3. Wait for CI to complete
4. Check that staging deployment triggers automatically
5. Verify deployment completes successfully

**Checklist:**
- [ ] CI pipeline completes on `develop`
- [ ] Staging deployment starts automatically
- [ ] Deployment completes without errors
- [ ] Application is accessible in staging

### Test Production Deployment

1. Merge `develop` to `main` branch
2. Wait for CI to complete
3. Check that production deployment waits for approval
4. Approve the deployment
5. Verify blue-green deployment completes
6. Check that release is created

**Checklist:**
- [ ] CI pipeline completes on `main`
- [ ] Production deployment waits for approval
- [ ] Approval notification sent to reviewers
- [ ] Deployment completes after approval
- [ ] Blue-green switch successful
- [ ] GitHub release created
- [ ] Slack notification sent
- [ ] Status page updated

## Step 8: Monitor First Deployment

After your first production deployment, verify:

- [ ] Deployment metrics are visible in GitHub Actions
- [ ] Slack notifications are received
- [ ] Status page reflects correct status
- [ ] Application health checks pass
- [ ] Load balancer is healthy
- [ ] No rollback triggered

## Troubleshooting

### Common Issues

**1. Environments not showing in GitHub UI**
- Ensure you have admin access to the repository
- Try creating manually via Settings → Environments

**2. Secrets not accessible in workflows**
- Verify secret names match exactly (case-sensitive)
- Check environment names are lowercase
- Ensure secrets are set at the correct level (repo vs environment)

**3. Deployment not triggering**
- Check CI pipeline completed successfully
- Verify branch names match (`develop` for staging, `main` for production)
- Review workflow run logs for conditions

**4. AWS authentication fails**
- Verify AWS credentials are valid
- Check AWS region is set correctly
- Ensure EKS cluster names are correct

**5. Approval not requested**
- Check production environment has required reviewers configured
- Verify reviewers have repository access
- Check notification settings

### Getting Help

1. Check workflow logs: `Actions` → Select the failed run
2. Verify configuration: Run `.github/scripts/verify-secrets.sh`
3. Review documentation: `.github/ENVIRONMENTS.md`
4. Check GitHub Actions documentation: https://docs.github.com/en/actions

## Security Best Practices

- [ ] Never commit secrets to the repository
- [ ] Rotate AWS credentials regularly (every 90 days)
- [ ] Use least-privilege IAM roles for AWS access
- [ ] Enable 2FA for all team members with deployment access
- [ ] Audit environment access regularly
- [ ] Keep third-party tokens secure and rotate periodically
- [ ] Monitor deployment logs for suspicious activity

## Maintenance

**Monthly:**
- [ ] Review and audit environment access
- [ ] Check for unused secrets
- [ ] Verify deployment approval process

**Quarterly:**
- [ ] Rotate AWS credentials
- [ ] Update third-party tokens
- [ ] Review and update protection rules
- [ ] Audit deployment history

**Annually:**
- [ ] Review and update deployment workflow
- [ ] Evaluate new security features
- [ ] Update documentation

---

**Status:**
- [ ] Setup in progress
- [ ] Setup complete
- [ ] Production deployments active

**Completed by:** _______________
**Date:** _______________
**Reviewed by:** _______________
