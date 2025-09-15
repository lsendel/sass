# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **SASS** (Specify-Analyze-Structure-Synthesize) repository - a feature development framework using Claude Code commands for systematic software engineering. The project is set up as a Java project with IntelliJ IDEA configuration but currently has an empty `src/` directory, suggesting it's a template or starter project.

## Development Workflow

### Feature Development Commands

The repository provides three main Claude Code commands for feature development:

1. **`/specify`** - Create feature specifications from natural language descriptions
2. **`/plan`** - Generate implementation plans with design artifacts
3. **`/tasks`** - Create actionable, dependency-ordered task lists

### Command Usage

```bash
# Start new feature development
/specify "description of your feature"

# Generate implementation plan
/plan "implementation details"

# Create actionable tasks
/tasks "context for task generation"
```

## Repository Structure

```
.specify/
├── scripts/bash/           # Feature development automation scripts
│   ├── create-new-feature.sh
│   ├── setup-plan.sh
│   ├── check-task-prerequisites.sh
│   └── common.sh
├── templates/              # Templates for specifications and plans
│   ├── spec-template.md
│   ├── plan-template.md
│   └── tasks-template.md
└── memory/
    └── constitution.md     # Project principles and constraints

.claude/commands/           # Claude Code command definitions
├── specify.md
├── plan.md
└── tasks.md
```

## Key Development Scripts

All scripts are located in `.specify/scripts/bash/` and should be run from the repository root:

- **`create-new-feature.sh`** - Creates new feature branches and initializes spec files
- **`setup-plan.sh`** - Sets up implementation planning workflow
- **`check-task-prerequisites.sh`** - Validates prerequisites for task generation
- **`common.sh`** - Shared functions for all scripts

## Feature Branch Convention

Feature branches follow the pattern: `###-feature-name` (e.g., `001-user-authentication`)

## Architecture Principles

The SASS framework follows these core principles:

1. **Specification First** - Features start with clear requirements before implementation
2. **Test-Driven Development** - Tests are written before implementation
3. **Systematic Planning** - Implementation follows structured planning phases
4. **Dependency Ordering** - Tasks are organized by dependencies for efficient execution

## Development Process

1. **Specify Phase** - Define what the feature should do (business requirements)
2. **Analyze Phase** - Plan how to implement it (technical design)
3. **Structure Phase** - Break down into actionable tasks
4. **Synthesize Phase** - Execute tasks and integrate components

## File Paths

All scripts expect absolute file paths. The common functions in `common.sh` help generate correct paths:

- `REPO_ROOT` - Repository root directory
- `FEATURE_DIR` - Feature-specific directory under `specs/`
- `FEATURE_SPEC` - Feature specification file
- `IMPL_PLAN` - Implementation plan file
- `TASKS` - Task breakdown file

## Java Project Setup

This appears to be a Java project template:
- Uses IntelliJ IDEA (`.idea/` configuration present)
- Has `sass.iml` module file
- `src/` directory exists but is empty
- No build configuration files (Maven/Gradle) detected yet