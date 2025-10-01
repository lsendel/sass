# Quick Reference Card

**Last Updated:** 2025-10-01
**Status:** ✅ All errors fixed, environments configured, ready to deploy

---

## 🎯 Current Status

```
Errors:      0 ✅
Warnings:    40 (safe - unconfigured optional secrets)
Environments: staging ✅  production ✅
Secrets:     14 configured (some placeholders)
```

---

## 📚 Key Documents

| Document | Purpose | When to Use |
|----------|---------|-------------|
| `FINAL-STATUS.md` | Complete status overview | Check what's done |
| `.github/REMAINING-SETUP.md` | Tasks before deploying | Before first deploy |
| `.github/QUICK-START.md` | Quick setup commands | Quick reference |
| `.github/ENVIRONMENTS.md` | Full environment guide | Deep dive |

---

## 🔧 Common Commands

### Check Configuration
```bash
# Verify all secrets and environments
.github/scripts/verify-secrets.sh

# List environments
gh api repos/lsendel/sass/environments --jq '.[].name'

# List secrets
gh secret list
gh secret list --env staging
gh secret list --env production
```

### Update Secrets
```bash
# Repository secrets
gh secret set AWS_REGION --body 'us-east-1'
gh secret set SONAR_TOKEN --body 'YOUR_TOKEN'
gh secret set DOCKER_USERNAME --body 'YOUR_USERNAME'
gh secret set DOCKER_PASSWORD --body 'YOUR_PASSWORD'

# Staging secrets
gh secret set AWS_ACCESS_KEY_ID --env staging --body 'YOUR_KEY'
gh secret set AWS_SECRET_ACCESS_KEY --env staging --body 'YOUR_SECRET'

# Production secrets
gh secret set AWS_ACCESS_KEY_ID_PROD --env production --body 'YOUR_KEY'
gh secret set AWS_SECRET_ACCESS_KEY_PROD --env production --body 'YOUR_SECRET'
```

### View Environments
```bash
# In browser
open https://github.com/lsendel/sass/settings/environments

# Via CLI
gh api repos/lsendel/sass/environments/staging
gh api repos/lsendel/sass/environments/production
```

### View Workflows
```bash
# In browser
open https://github.com/lsendel/sass/actions

# Via CLI
gh run list
gh run view <run-id>
```

---

## 🚀 Deployment Cheat Sheet

### Staging Deployment
```bash
# Make changes
git checkout develop
# ... make changes ...
git add .
git commit -m "feat: your feature"

# Deploy
git push origin develop

# Watch
open https://github.com/lsendel/sass/actions
```

### Production Deployment
```bash
# Merge to main
git checkout main
git merge develop
git push origin main

# Approve when prompted
# (Reviewers will get notification)

# Watch
open https://github.com/lsendel/sass/actions
```

---

## ⚠️ Before First Deployment

**Must Have:**
- [ ] Real AWS credentials for staging
- [ ] Real AWS credentials for production

**Should Have:**
- [ ] SonarCloud token (for code quality)
- [ ] Docker Hub credentials (for releases)
- [ ] Production reviewers configured

**Nice to Have:**
- [ ] Snyk token (security scanning)
- [ ] GitGuardian key (secret detection)
- [ ] Real Slack token (notifications)
- [ ] Real Statuspage credentials (status updates)

---

## 🔍 Troubleshooting

### Environments not showing?
```bash
# Verify they exist
gh api repos/lsendel/sass/environments

# Reload VS Code
# Cmd+Shift+P → "Reload Window"
```

### Secrets not working?
```bash
# Check they're set
gh secret list
gh secret list --env staging
gh secret list --env production

# Verify names match exactly (case-sensitive)
```

### Deployment failing?
```bash
# Check workflow logs
gh run list
gh run view <run-id> --log

# Verify CI passed first
# Staging requires develop branch
# Production requires main branch
```

### Need to rollback?
```bash
# Staging
kubectl rollout undo deployment/payment-platform -n staging

# Production (automatic in workflow if deployment fails)
# Or manual:
kubectl patch service payment-platform -n production \
  -p '{"spec":{"selector":{"version":"blue"}}}'
```

---

## 📊 Workflow Triggers

| Event | Branch | Environment | Approval | Auto Deploy |
|-------|--------|-------------|----------|-------------|
| Push | `develop` | staging | ❌ No | ✅ Yes |
| Push | `main` | production | ✅ Yes | After approval |
| Manual | Any | Any | Depends | No |

---

## 🎯 Success Criteria

**Staging deployment successful when:**
- ✅ CI passes
- ✅ Deployment completes
- ✅ Health checks pass
- ✅ Smoke tests pass
- ✅ Notification sent

**Production deployment successful when:**
- ✅ CI passes
- ✅ Approved by reviewers
- ✅ Blue-green deployment completes
- ✅ Traffic switched
- ✅ Monitoring period passes
- ✅ GitHub release created
- ✅ Notifications sent

---

## 🆘 Quick Help

| Issue | Solution |
|-------|----------|
| Errors in VS Code | Reload window (Cmd+Shift+P) |
| Missing secrets | Run `.github/scripts/verify-secrets.sh` |
| Deployment stuck | Check Actions logs |
| AWS auth fails | Update credentials with real values |
| Can't approve | Add yourself as reviewer |

---

## 📞 Resources

- **Environments:** https://github.com/lsendel/sass/settings/environments
- **Secrets:** https://github.com/lsendel/sass/settings/secrets/actions
- **Actions:** https://github.com/lsendel/sass/actions
- **Docs:** `.github/` directory

---

## 🔗 URLs

```
Repository:   https://github.com/lsendel/sass
Environments: https://github.com/lsendel/sass/settings/environments
Secrets:      https://github.com/lsendel/sass/settings/secrets/actions
Actions:      https://github.com/lsendel/sass/actions
```

---

## 💡 Pro Tips

1. **Always test in staging first** before deploying to production
2. **Use `verify-secrets.sh`** before important deployments
3. **Check Actions logs** if deployment fails
4. **Keep credentials secure** - never commit them
5. **Rotate AWS keys** every 90 days
6. **Add reviewers** for production deployments
7. **Monitor first deployments** closely
8. **Use Slack notifications** to stay informed

---

**Remember:** You're 100% set up. Just add real AWS credentials when ready to deploy! 🚀
