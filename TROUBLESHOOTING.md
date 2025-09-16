Git index.lock errors
---------------------

Problem
- Error: "Unable to create '.git/index.lock': File exists".
- Cause: a previous Git operation crashed/was interrupted, or a background/IDE Git process overlapped with your command, leaving a stale lock.

Quick fix
- Remove the stale lock:
  - Preferred: `make git-clean-lock` (uses scripts/git-clean-lock.sh safety checks)
  - Force (only if certain no Git operation is running): `make git-clean-lock-force`

Prevention
- Avoid running multiple Git operations concurrently (CLI + IDE at the same time).
- Keep pre-commit hooks and Git tools responsive; kill hung editors spawned by `git commit`.
- We disable background Git maintenance locally in this repo to reduce surprise locking.

Details
- The script checks whether any process holds `.git/index.lock` (via lsof if available),
  considers the lock stale if older than 10 minutes, and removes it if safe.
- If in doubt, it refuses to delete; use `--force` only when you are sure.

