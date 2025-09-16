#!/usr/bin/env bash
set -euo pipefail

repo_root=$(git rev-parse --show-toplevel 2>/dev/null || echo "")
if [[ -z "${repo_root}" ]]; then
  echo "Error: Not inside a Git repository." >&2
  exit 2
fi

lock_file="${repo_root}/.git/index.lock"

usage() {
  cat <<'USAGE'
Usage: scripts/git-clean-lock.sh [--force] [--max-age MINUTES]

Safely removes a stale Git index.lock if no active Git process holds it.

Options:
  --force           Remove lock without checks (use with caution)
  --max-age MINUTES Consider lock stale if older than MINUTES (default: 10)

Exit codes:
  0  removed or nothing to do
  1  lock present and appears in-use; not removed
  2  not a git repo or other error
USAGE
}

force=0
max_age_min=10

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      force=1
      shift
      ;;
    --max-age)
      max_age_min=${2:-10}
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

if [[ ! -e "$lock_file" ]]; then
  echo "No index.lock present. Nothing to do."
  exit 0
fi

if [[ "$force" -eq 1 ]]; then
  rm -f "$lock_file"
  echo "Removed index.lock (forced)."
  exit 0
fi

# Determine lock age in minutes
now_epoch=$(date +%s)
lock_epoch=$(stat -f %m "$lock_file" 2>/dev/null || stat -c %Y "$lock_file" 2>/dev/null || echo 0)
age_min=$(( (now_epoch - lock_epoch) / 60 ))

# Check for any running git processes (best-effort)
git_procs=$(pgrep -fl "^(git|/.*git)( |$)" 2>/dev/null || true)

# If lsof is available, see if any process holds the lock specifically
holding_desc=""
if command -v lsof >/dev/null 2>&1; then
  holding_desc=$(lsof +c 0 -- "$lock_file" 2>/dev/null || true)
fi

if [[ -n "$holding_desc" ]]; then
  echo "index.lock appears to be in use by:"
  echo "$holding_desc"
  echo "Not removing. Re-run with --force if you are certain it is stale."
  exit 1
fi

if [[ -n "$git_procs" && $age_min -lt $max_age_min ]]; then
  echo "Git processes detected and index.lock is recent (${age_min}m)."
  echo "Not removing. If these are stale, re-run after they exit or use --force."
  echo "$git_procs"
  exit 1
fi

if [[ $age_min -ge $max_age_min ]]; then
  rm -f "$lock_file"
  echo "Removed stale index.lock (age ${age_min}m >= ${max_age_min}m)."
  exit 0
fi

# No holders found and either no git processes or very recent lock; remove conservatively
rm -f "$lock_file"
echo "Removed index.lock (no holder detected)."
exit 0

