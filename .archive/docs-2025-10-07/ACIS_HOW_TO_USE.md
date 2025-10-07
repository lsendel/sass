# ACIS - How to Use

## Simple Guide to Automated Code Quality

**Version**: 2.0.0
**Status**: ✅ Ready to Use

---

## 🎯 What Is This?

ACIS is an **automated code quality system** that runs in **3 ways**:

1. **On Your Computer** - Checks your code when you commit
2. **On a Timer** - Runs every hour to improve code automatically
3. **In GitHub Actions** - Validates PRs and creates improvement PRs

---

## 🚀 Quick Setup (5 Minutes)

### Step 1: Install Git Hooks

```bash
./scripts/acis/git-hooks/install-hooks.sh install
```

**What this does**: Checks your code before you commit or push.

### Step 2: Enable GitHub Actions

Already done! The workflow file is at `.github/workflows/acis-automated.yml`

**What this does**:

- Validates every PR
- Runs hourly to find and fix issues
- Creates PRs with improvements

### Step 3: Done! 🎉

That's it. The system now works automatically.

---

## 📋 How It Works

### When You Commit

```bash
git add .
git commit -m "feat: my changes"
```

**What happens**:

1. ✅ Checks if code compiles
2. ✅ Fixes code style automatically
3. ✅ Runs fast tests
4. ✅ Blocks commit if something is broken
5. ⏱️ Takes 30-60 seconds

### When You Push

```bash
git push
```

**What happens**:

1. ✅ Full compilation check
2. ✅ All tests run
3. ✅ Security scan
4. ✅ Architecture validation
5. ✅ Blocks push if something is broken
6. ⏱️ Takes 5-15 minutes

### Every Hour (GitHub Actions)

**What happens**:

1. ✅ Scans entire codebase
2. ✅ Fixes issues automatically
3. ✅ Runs all tests
4. ✅ Creates PR with improvements
5. ⏱️ Runs in background

### On Pull Requests

**What happens**:

1. ✅ Validates all changes
2. ✅ Comments on PR with results
3. ✅ Blocks merge if validation fails
4. ⏱️ Takes 10-20 minutes

---

## ⚙️ Configuration

**File**: `scripts/acis/config/acis-config.yml`

### Change Timer Interval

```yaml
scheduler:
  interval_hours: 1 # Change to 2, 4, 6, 12, or 24
```

Then update `.github/workflows/acis-automated.yml`:

```yaml
schedule:
  - cron: "0 */1 * * *" # Change */1 to */2 for every 2 hours
```

### Enable/Disable Features

```yaml
# Turn off git hooks
git_hooks:
  enabled: false

# Turn off pre-commit (keep pre-push)
git_hooks:
  pre_commit:
    enabled: false

# Turn off auto-fix
git_hooks:
  pre_commit:
    auto_fix: false
```

---

## 🔧 Common Tasks

### Skip Validation (Emergency Only)

```bash
# Skip pre-commit hook
git commit --no-verify -m "emergency fix"

# Skip pre-push hook
git push --no-verify

# Or use environment variable
ACIS_SKIP_HOOKS=true git commit -m "WIP"
```

### Check Hook Status

```bash
./scripts/acis/git-hooks/install-hooks.sh status
```

### Uninstall Hooks

```bash
./scripts/acis/git-hooks/install-hooks.sh uninstall
```

### View Logs

```bash
# Git hook logs
cat .acis/logs/acis.log

# GitHub Actions logs
# Go to Actions tab in GitHub
```

### Manual Run

```bash
# Dry run (no changes)
ACIS_CONTINUE_APPROVED=true DRY_RUN=true ./scripts/acis/acis-main.sh

# Real run
ACIS_CONTINUE_APPROVED=true ./scripts/acis/acis-main.sh
```

---

## 📊 What Gets Checked?

### Pre-Commit (Fast - 1 min)

- ✅ Java compilation
- ✅ Code style (auto-fixed)
- ✅ Unit tests

### Pre-Push (Full - 10 min)

- ✅ Java compilation
- ✅ Code style
- ✅ All tests
- ✅ Test coverage ≥ 85%
- ✅ Security scan
- ✅ Architecture rules

### Hourly Background (Comprehensive)

- ✅ Everything above
- ✅ Auto-fixes issues
- ✅ Creates PR with improvements

---

## 🐛 Troubleshooting

### "Hook not running"

```bash
# Reinstall hooks
./scripts/acis/git-hooks/install-hooks.sh uninstall
./scripts/acis/git-hooks/install-hooks.sh install
```

### "Validation too slow"

```yaml
# Edit scripts/acis/config/acis-config.yml
git_hooks:
  pre_commit:
    gates:
      - compilation
      - code_style
      # Remove: fast_tests (to make it faster)
```

### "GitHub Actions failing"

1. Check workflow file: `.github/workflows/acis-automated.yml`
2. View logs in GitHub Actions tab
3. Ensure `pyyaml` is installed: `pip3 install pyyaml`

---

## 📁 File Locations

```
Key Files:
├── scripts/acis/config/acis-config.yml     # Main configuration
├── scripts/acis/acis-main.sh               # Main script
├── scripts/acis/git-hooks/pre-commit       # Pre-commit hook
├── scripts/acis/git-hooks/pre-push         # Pre-push hook
├── .github/workflows/acis-automated.yml    # GitHub Actions
└── .acis/logs/acis.log                     # Logs
```

---

## ✅ Best Practices

1. **Don't skip hooks** unless it's an emergency
2. **Review automated PRs** before merging
3. **Keep config simple** - defaults work well
4. **Check logs** if something seems wrong
5. **Update regularly** - pull latest changes

---

## 🎓 Learn More

- **Full Architecture**: See `AUTOMATED_CONTINUOUS_IMPROVEMENT_SYSTEM.md`
- **Git Hooks Detail**: See `ACIS_GIT_HOOKS_INTEGRATION.md`
- **Complete Summary**: See `ACIS_COMPLETE_SYSTEM_SUMMARY.md`

---

## 📞 Quick Reference

| Task            | Command                                                   |
| --------------- | --------------------------------------------------------- |
| Install hooks   | `./scripts/acis/git-hooks/install-hooks.sh install`       |
| Check status    | `./scripts/acis/git-hooks/install-hooks.sh status`        |
| Uninstall hooks | `./scripts/acis/git-hooks/install-hooks.sh uninstall`     |
| Skip hook       | `git commit --no-verify`                                  |
| View logs       | `cat .acis/logs/acis.log`                                 |
| Manual run      | `ACIS_CONTINUE_APPROVED=true ./scripts/acis/acis-main.sh` |
| Edit config     | `vi scripts/acis/config/acis-config.yml`                  |

---

**That's it! The system is simple to use and powerful.** 🚀

Last Updated: 2025-10-02
