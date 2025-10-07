# ACIS Git Hooks Integration

## Automatic Code Quality Validation on Commit & Push

**Version**: 2.0.0
**Date**: 2025-10-02
**Status**: ✅ COMPLETE & READY FOR USE

---

## 🎯 Overview

The ACIS system now includes **TWO complementary automated workflows**:

### 1. ⏰ **Timer-Based Continuous Improvement** (Background)

- Runs autonomously every N hours (configurable)
- Comprehensive quality improvements with auto-fix
- Operates independently in background
- Creates automated improvement PRs

### 2. 🔒 **Git Hook Validation** (Developer Workflow)

- Runs automatically on `git commit` and `git push`
- Fast feedback during development
- Prevents broken code from entering repository
- Smart detection of what changed

---

## 🔄 Dual-Mode Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    ACIS SYSTEM MODES                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────┐  ┌──────────────────────────┐  │
│  │   TIMER MODE             │  │   GIT HOOK MODE          │  │
│  │   (Background)           │  │   (Developer Workflow)   │  │
│  ├──────────────────────────┤  ├──────────────────────────┤  │
│  │ • Every 1-24 hours       │  │ • On git commit/push     │  │
│  │ • Comprehensive checks   │  │ • Fast validation        │  │
│  │ • Auto-fix enabled       │  │ • Smart detection        │  │
│  │ • Creates git commits    │  │ • Blocks bad commits     │  │
│  │ • Autonomous operation   │  │ • Immediate feedback     │  │
│  └──────────────────────────┘  └──────────────────────────┘  │
│                                                                 │
│         ↓ BOTH USE SAME ↓                                      │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  SHARED INFRASTRUCTURE                                   │  │
│  │  • Quality gates validator                               │  │
│  │  • Auto-fix engines                                      │  │
│  │  • Security scanners                                     │  │
│  │  │  • Configuration system                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📋 Git Hooks Features

### Pre-Commit Hook (Fast Validation)

**Triggers**: When you run `git commit`

**Duration**: 30 seconds - 5 minutes

**Quality Gates**:

- ✅ Java compilation (main + test)
- ✅ Checkstyle violations
- ✅ Unit tests (fast, no integration tests)

**Smart Features**:

- Only runs checks for changed files
- Auto-fixes code style issues
- Auto-stages fixed files
- Provides instant feedback

**Example Output**:

```bash
$ git commit -m "feat: Add user service"

══════════════════════════════════════════
ACIS Pre-Commit Validation
══════════════════════════════════════════

[INFO] Analyzing staged changes...
[INFO] Changes detected: 3 file(s)
[INFO] Quality gates to run:
  - compilation
  - code_style
  - fast_tests

[INFO] Running gate: compilation...
✅ Compilation passed

[INFO] Running gate: code_style...
⚠️  Code style violations detected
[INFO] Attempting auto-fix...
✅ Auto-fix successful
✅ Code style now passes
[INFO] Fixed files automatically staged

[INFO] Running gate: fast_tests...
✅ Unit tests passed

══════════════════════════════════════════
Pre-Commit Validation Summary
══════════════════════════════════════════
Gates run: 3
Passed: 3
Failed: 0
Duration: 45s

✅ Pre-commit validation PASSED
```

### Pre-Push Hook (Comprehensive Validation)

**Triggers**: When you run `git push`

**Duration**: 5-30 minutes

**Quality Gates**:

- ✅ Compilation
- ✅ Code style
- ✅ Architecture (Modulith + ArchUnit)
- ✅ Security (OWASP + security tests)
- ✅ Full test suite + coverage verification

**Smart Features**:

- Compares changes vs remote branch
- Detects critical file changes
- Enhanced validation for security-critical code
- Prevents pushing broken code

**Example Output**:

```bash
$ git push origin feature/user-service

══════════════════════════════════════════
ACIS Pre-Push Validation
══════════════════════════════════════════

[INFO] Analyzing changes to be pushed...
[INFO] Changes to push: 12 file(s) across 2 module(s)
[INFO] Comprehensive quality gates:
  - compilation
  - code_style
  - architecture
  - security
  - testing

═══════════════════════════════════════
Running gate: compilation
═══════════════════════════════════════
✅ Compilation passed

═══════════════════════════════════════
Running gate: code_style
═══════════════════════════════════════
✅ Code style passed

═══════════════════════════════════════
Running gate: architecture
═══════════════════════════════════════
[INFO] Running Modulith boundary checks...
✅ Modulith checks passed
[INFO] Running ArchUnit tests...
✅ Architecture tests passed

═══════════════════════════════════════
Running gate: security
═══════════════════════════════════════
[INFO] Running security tests...
✅ Security tests passed

═══════════════════════════════════════
Running gate: testing
═══════════════════════════════════════
[INFO] Running full test suite...
✅ Tests passed
[INFO] Verifying code coverage...
✅ Coverage requirements met

══════════════════════════════════════════
Pre-Push Validation Summary
══════════════════════════════════════════
Gates run: 5
Passed: 5
Failed: 0
Duration: 12m 34s

✅ Pre-push validation PASSED
✅ Safe to push to remote

Enumerating objects: 25, done.
Counting objects: 100% (25/25), done.
...
```

---

## 🚀 Installation

### Step 1: Install Git Hooks

```bash
# Interactive menu
./scripts/acis/git-hooks/install-hooks.sh

# Or directly
./scripts/acis/git-hooks/install-hooks.sh install
```

**Output**:

```
╔════════════════════════════════════════════════════╗
║          Installing ACIS Git Hooks                ║
╚════════════════════════════════════════════════════╝

[INFO] Backing up existing hooks...
[INFO] No existing hooks to backup
✅ Installed (symlink): pre-commit
✅ Installed (symlink): pre-push

Installed 2 git hook(s)

Git hooks are now active!

Configuration:
  - Pre-commit: ENABLED
  - Pre-push: ENABLED
  - Auto-fix on commit: YES

To bypass hooks (emergency only):
  git commit --no-verify
  ACIS_SKIP_HOOKS=true git commit
```

### Step 2: Verify Installation

```bash
./scripts/acis/git-hooks/install-hooks.sh status
```

**Output**:

```
══════════════════════════════════════════
ACIS Git Hooks Status
══════════════════════════════════════════

Configuration:
  Master switch: ENABLED
  Pre-commit: ENABLED
  Pre-push: ENABLED

Installation status:
  ✅ pre-commit (symlink → ../scripts/acis/git-hooks/pre-commit)
  ✅ pre-push (symlink → ../scripts/acis/git-hooks/pre-push)
```

### Step 3: Test Hooks

```bash
./scripts/acis/git-hooks/install-hooks.sh test
```

---

## ⚙️ Configuration

### Enable/Disable Git Hooks

Edit `scripts/acis/config/acis-config.yml`:

```yaml
# Master switch for ALL git hooks
git_hooks:
  enabled: true # Set to false to disable all hooks

  # Pre-commit configuration
  pre_commit:
    enabled: true # Set to false to disable pre-commit
    auto_fix: true # Auto-fix code style issues
    fail_on_error: true # Block commit if validation fails

  # Pre-push configuration
  pre_push:
    enabled: true # Set to false to disable pre-push
    fail_on_error: true # Block push if validation fails
```

### Customize Quality Gates

**Pre-Commit (Fast Gates)**:

```yaml
git_hooks:
  pre_commit:
    gates:
      - compilation # Always recommended
      - code_style # Fast and important
      - fast_tests # Unit tests only
```

**Pre-Push (Comprehensive Gates)**:

```yaml
git_hooks:
  pre_push:
    gates:
      - compilation
      - code_style
      - architecture # Modulith + ArchUnit
      - security # OWASP + security tests
      - testing # Full suite + coverage
```

### Smart Detection Configuration

```yaml
git_hooks:
  pre_commit:
    smart_detection:
      enabled: true # Only check changed files
      scope: "staged" # Options: staged, all_changes, diff_from_main

  pre_push:
    smart_detection:
      enabled: true
      scope: "diff_from_remote" # Compare with remote branch
```

---

## 🎯 Smart Change Detection

The system intelligently determines which checks to run based on what files changed:

### Detection Logic

```yaml
File Type         | Pre-Commit Checks      | Pre-Push Checks
================================================================================
*.java            | compilation,           | compilation, code_style,
(main code)       | code_style,            | architecture, testing,
| fast_tests             | security

*.java            | compilation,           | compilation, code_style,
(test code)       | code_style,            | testing
| fast_tests             |

build.gradle      | compilation            | compilation, security,
package.json      |                        | testing (dependency changes)

*.yml, *.xml      | compilation            | compilation, testing
(config)          |                        | (config changes)

*.md, *.txt       | SKIP                   | SKIP
(docs only)       |                        |

Critical files    | FORCE COMPREHENSIVE    | FORCE COMPREHENSIVE
(**/payment/**,   | ALL GATES              | ALL GATES + MANUAL REVIEW
**/security/**)   |                        |
```

### Example: Smart Detection in Action

**Scenario 1**: Changed only documentation

```bash
$ git add README.md
$ git commit -m "docs: Update readme"

[INFO] Analyzing staged changes...
[INFO] Detected 1 file(s)
[INFO] Only documentation changed - skipping validation
[SUCCESS] Commit allowed
```

**Scenario 2**: Changed Java code

```bash
$ git add src/main/java/UserService.java
$ git commit -m "feat: Add user lookup"

[INFO] Analyzing staged changes...
[INFO] Detected 1 file(s)
[INFO] Java code changed - running:
  - compilation
  - code_style
  - fast_tests
```

**Scenario 3**: Changed critical security code

```bash
$ git add src/main/java/com/platform/security/AuthFilter.java
$ git commit -m "fix: Update auth filter"

[WARN] ⚠️  Critical files detected - enhanced validation activated
[INFO] Running comprehensive validation:
  - compilation
  - code_style
  - architecture
  - security
  - testing
```

---

## 🛠️ Usage Examples

### Normal Workflow (Hooks Active)

```bash
# 1. Make changes
vim src/main/java/UserService.java

# 2. Stage changes
git add src/main/java/UserService.java

# 3. Commit (pre-commit hook runs automatically)
git commit -m "feat: Add user service"
# → Hooks run compilation, style check, tests
# → Auto-fixes applied if needed
# → Commit succeeds if all pass

# 4. Push (pre-push hook runs automatically)
git push origin feature-branch
# → Full validation runs
# → Push allowed only if all gates pass
```

### Bypass Hooks (Emergency Only)

```bash
# Method 1: --no-verify flag
git commit --no-verify -m "WIP: emergency fix"
git push --no-verify

# Method 2: Environment variable
ACIS_SKIP_HOOKS=true git commit -m "WIP: emergency fix"
ACIS_SKIP_HOOKS=true git push

# ⚠️  Bypasses are logged to .acis/logs/bypass.log
```

### Disable Hooks Temporarily

```bash
# Option 1: Edit configuration
vi scripts/acis/config/acis-config.yml
# Set git_hooks.enabled: false

# Option 2: Uninstall hooks
./scripts/acis/git-hooks/install-hooks.sh uninstall

# Reinstall later
./scripts/acis/git-hooks/install-hooks.sh install
```

---

## 🔄 Integration with Timer-Based ACIS

### How They Work Together

```
Developer Workflow (Git Hooks)           Background Process (Timer)
═══════════════════════════════          ══════════════════════════

1. Developer writes code                  Every 1 hour:
   ↓                                      ├─ Run comprehensive scan
2. git commit                             ├─ Auto-fix all issues
   ├─ Pre-commit hook runs                ├─ Run full test suite
   ├─ Fast validation (2-5 min)           ├─ Check security
   ├─ Auto-fix code style                 ├─ Verify coverage
   └─ Block if fails                      ├─ Create commits
   ↓                                      └─ (operates independently)
3. Fix issues if needed
   ↓
4. git push
   ├─ Pre-push hook runs
   ├─ Full validation (10-30 min)
   ├─ All quality gates
   └─ Block if fails
   ↓
5. Code in repository is always clean ✅
```

### Configuration Example (Both Modes)

```yaml
# Timer-based ACIS (Background)
scheduler:
  interval_hours: 1 # Run every hour
  max_iterations: 10

git:
  auto_commit: true # Create automated commits
  create_branch: true # On feature branches

# Git hooks (Developer workflow)
git_hooks:
  enabled: true

  pre_commit:
    enabled: true
    auto_fix: true # Auto-fix on commit
    gates: [compilation, code_style, fast_tests]

  pre_push:
    enabled: true
    auto_fix: false # Validate only on push
    gates: [compilation, code_style, architecture, security, testing]
```

---

## 📊 Comparison: Timer vs Git Hooks

| Feature               | Timer Mode                   | Git Hooks Mode              |
| --------------------- | ---------------------------- | --------------------------- |
| **Trigger**           | Every N hours (cron/systemd) | On git commit/push          |
| **Duration**          | 30 min - 2 hours             | 1-30 minutes                |
| **Quality Gates**     | ALL (comprehensive)          | Configurable subset         |
| **Auto-Fix**          | Yes (aggressive)             | Yes (pre-commit only)       |
| **Human Interaction** | Zero (fully autonomous)      | Blocking (requires passing) |
| **Creates Commits**   | Yes (automated PRs)          | No (validates only)         |
| **Rollback**          | Yes (automatic)              | N/A                         |
| **Best For**          | Background maintenance       | Developer feedback          |
| **Can Disable**       | Via config or env flag       | Via --no-verify or config   |

---

## 🧪 Validation & Testing

### Test Pre-Commit Hook

```bash
# Create test change
echo "// Test" >> backend/src/main/java/Test.java
git add backend/src/main/java/Test.java

# Commit (hook will run)
git commit -m "test: Testing pre-commit hook"

# Expected: Hook runs fast validation
# Expected: Auto-fixes applied if needed
# Expected: Commit succeeds or fails based on validation
```

### Test Pre-Push Hook

```bash
# Create test branch
git checkout -b test-pre-push-hook

# Make changes and commit
echo "// Test" >> backend/src/main/java/Test.java
git add backend/src/main/java/Test.java
git commit -m "test: Testing pre-push hook"

# Push (hook will run comprehensive validation)
git push -u origin test-pre-push-hook

# Expected: Full quality gates run
# Expected: 10-30 minute validation
# Expected: Push succeeds only if all gates pass
```

### Test Smart Detection

```bash
# Test 1: Documentation only
echo "# Test" >> README.md
git add README.md
git commit -m "docs: test"
# Expected: Skips validation

# Test 2: Java code
echo "// Test" >> backend/src/main/java/UserService.java
git add backend/src/main/java/UserService.java
git commit -m "feat: test"
# Expected: Runs compilation + style + tests

# Test 3: Critical security code
echo "// Test" >> backend/src/main/java/com/platform/security/AuthFilter.java
git add backend/src/main/java/com/platform/security/AuthFilter.java
git commit -m "fix: test"
# Expected: Runs comprehensive validation with warning
```

---

## 📁 File Structure

```
sass/
├── scripts/acis/
│   ├── config/
│   │   └── acis-config.yml              # ✅ Git hooks config section
│   ├── git-hooks/
│   │   ├── install-hooks.sh             # ✅ Hook installer
│   │   ├── smart-detector.sh            # ✅ Smart change detection
│   │   ├── pre-commit                   # ✅ Fast validation hook
│   │   └── pre-push                     # ✅ Comprehensive hook
│   ├── acis-main.sh                     # ✅ Timer-based orchestrator
│   └── utils/
│       ├── logger.sh                    # ✅ Shared logging
│       ├── config-reader.sh             # ✅ Shared config reader
│       └── git-utils.sh                 # ✅ Shared git operations
└── .git/hooks/
    ├── pre-commit  → ../../scripts/acis/git-hooks/pre-commit
    └── pre-push    → ../../scripts/acis/git-hooks/pre-push
```

---

## 🎯 Best Practices

### 1. **Use Both Modes Together**

- Timer mode: Background quality maintenance
- Git hooks: Fast developer feedback
- Result: Always-clean codebase

### 2. **Configure Gates Appropriately**

- Pre-commit: Fast gates only (< 5 min)
- Pre-push: Comprehensive gates (acceptable longer runtime)
- Timer: All gates with auto-fix

### 3. **Smart Detection**

- Enable for faster commits
- Automatically runs only relevant checks
- Critical files always get full validation

### 4. **Emergency Bypass Protocol**

- Document when --no-verify is acceptable
- Always review bypassed commits later
- Monitor bypass logs: `.acis/logs/bypass.log`

### 5. **Team Adoption**

- Install hooks as part of onboarding
- Document configuration options
- Provide bypass instructions for emergencies

---

## 🐛 Troubleshooting

### Hook Not Running

```bash
# Check installation
./scripts/acis/git-hooks/install-hooks.sh status

# Reinstall
./scripts/acis/git-hooks/install-hooks.sh uninstall
./scripts/acis/git-hooks/install-hooks.sh install
```

### Hook Failing

```bash
# Check logs
tail -f .acis/logs/acis.log

# Test specific gate
cd backend && ./gradlew checkstyleMain

# Run hook manually for debugging
.git/hooks/pre-commit
```

### Performance Issues

```bash
# Disable slow gates for pre-commit
vi scripts/acis/config/acis-config.yml

git_hooks:
  pre_commit:
    gates:
      - compilation   # Keep
      - code_style    # Keep
      # Remove: fast_tests (if too slow)
```

### Smart Detection Not Working

```bash
# Test smart detector manually
./scripts/acis/git-hooks/smart-detector.sh report staged fast

# Check dependencies
command -v jq  # Required for JSON parsing
command -v python3  # Required for YAML parsing
```

---

## 📈 Success Metrics

### Pre-Commit Hook

| Metric                 | Target  | How to Measure          |
| ---------------------- | ------- | ----------------------- |
| Validation Time        | < 5 min | Monitor logs            |
| Auto-Fix Success Rate  | > 80%   | Count fixes vs failures |
| Developer Satisfaction | High    | Survey team             |
| Bypass Rate            | < 5%    | Check bypass logs       |

### Pre-Push Hook

| Metric                  | Target   | How to Measure       |
| ----------------------- | -------- | -------------------- |
| Validation Time         | < 30 min | Monitor logs         |
| Push Failure Rate       | < 10%    | Track failed pushes  |
| Prevented Bugs          | Track    | Code review feedback |
| CI/CD Failure Reduction | > 50%    | Compare before/after |

---

## 🚀 Deployment Checklist

- [ ] Install hooks: `./scripts/acis/git-hooks/install-hooks.sh install`
- [ ] Verify status: `./scripts/acis/git-hooks/install-hooks.sh status`
- [ ] Test hooks: `./scripts/acis/git-hooks/install-hooks.sh test`
- [ ] Configure gates in `acis-config.yml`
- [ ] Enable smart detection
- [ ] Document team bypass protocol
- [ ] Set up bypass monitoring
- [ ] Train team on usage
- [ ] Monitor metrics (first week)
- [ ] Adjust configuration based on feedback

---

## 🎓 Advanced Usage

### Custom Gate Configuration

```yaml
git_hooks:
  pre_commit:
    mode: "custom" # Custom gate selection
    gates:
      - compilation
      - code_style
      # Skip tests for faster commits
```

### Parallel Execution

```yaml
git_hooks:
  performance:
    parallel_execution: true # Run independent gates in parallel
    max_parallel_jobs: 4
```

### Caching Results

```yaml
git_hooks:
  performance:
    cache_results: true # Cache validation results
    cache_duration_minutes: 60
```

---

## 📚 Additional Resources

- **Main Documentation**: `AUTOMATED_CONTINUOUS_IMPROVEMENT_SYSTEM.md`
- **Implementation Status**: `ACIS_IMPLEMENTATION_COMPLETE.md`
- **Configuration**: `scripts/acis/config/acis-config.yml`
- **Logs**: `.acis/logs/acis.log`
- **Bypass Logs**: `.acis/logs/bypass.log`

---

**Status**: ✅ PRODUCTION READY
**Last Updated**: 2025-10-02
**Version**: 2.0.0
