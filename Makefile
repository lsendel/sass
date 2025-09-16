SHELL := /bin/bash

.PHONY: git-clean-lock
git-clean-lock:
	@bash scripts/git-clean-lock.sh

.PHONY: git-clean-lock-force
git-clean-lock-force:
	@bash scripts/git-clean-lock.sh --force

