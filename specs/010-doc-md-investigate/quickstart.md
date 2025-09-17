# Documentation System Quickstart Guide

## Prerequisites
- Node.js 18+ installed
- Git repository access
- Basic markdown knowledge

## Installation & Setup

### 1. Install Documentation Tools
```bash
# Navigate to project root
cd /path/to/your/project

# Install Docusaurus and dependencies
npm install -g @docusaurus/cli
npm install --save-dev markdown-link-check textlint markdownlint-cli
```

### 2. Initialize Documentation Structure
```bash
# Create docs directory with Docusaurus
npx docusaurus init docs classic

# Navigate to docs directory
cd docs

# Install additional dependencies
npm install --save @docusaurus/plugin-content-docs @algolia/docsearch-react
```

### 3. Configure Documentation System
```bash
# Copy configuration template (to be created)
cp ../templates/docusaurus.config.js ./docusaurus.config.js

# Create documentation structure
mkdir -p docs/{architecture,backend,frontend,guides}
mkdir -p static/img/{architecture,diagrams,screenshots}
```

## Creating Your First Documentation Page

### 1. Create Architecture Overview
```bash
# Use the docs-create command (to be implemented)
docs-create architecture "System Architecture Overview" \
  --section architecture \
  --author "Your Name"

# Alternative: Manual creation
cat > docs/architecture/system-overview.md << 'EOF'
---
title: System Architecture Overview
description: High-level system architecture and component relationships
lastUpdated: 2025-01-16
author: Your Name
version: 1.0.0
audience: developer
status: draft
---

# System Architecture Overview

## Introduction
Brief description of the system architecture...

## Components
### Frontend
- Technology stack
- Key responsibilities

### Backend
- Technology stack
- Key responsibilities

## Data Flow
Description of how data flows through the system...

## Deployment Architecture
How the system is deployed and scaled...
EOF
```

### 2. Validate Your Content
```bash
# Run validation (to be implemented)
docs-validate docs/architecture/system-overview.md

# Check for broken links
markdown-link-check docs/architecture/system-overview.md

# Lint markdown syntax
markdownlint docs/architecture/system-overview.md
```

### 3. Build and Preview
```bash
# Build documentation site
docs-build

# Start development server
docs-serve --port 3000 --open
```

## Creating Documentation for Different Audiences

### Developer Documentation
```bash
# Create API documentation
docs-create api "Authentication API" \
  --section backend \
  --author "Developer Name"

# Create setup guide
docs-create guide "Development Environment Setup" \
  --section guides \
  --author "Developer Name"
```

### Operator Documentation
```bash
# Create deployment guide
docs-create guide "Production Deployment" \
  --section operations \
  --author "DevOps Engineer"

# Create troubleshooting guide
docs-create troubleshooting "Common Issues and Solutions" \
  --section operations \
  --author "DevOps Engineer"
```

### Stakeholder Documentation
```bash
# Create feature overview
docs-create feature "User Management System" \
  --section features \
  --author "Product Manager"
```

## Adding Diagrams and Images

### 1. Architecture Diagrams
```bash
# Add diagram to static assets
cp your-architecture-diagram.png static/img/architecture/

# Reference in markdown
echo "![System Architecture](../static/img/architecture/your-architecture-diagram.png)" \
  >> docs/architecture/system-overview.md
```

### 2. Code Examples
```markdown
# In your documentation files, use code blocks with language specification
```java
@RestController
public class UserController {
    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }
}
```

## Setting Up Search

### 1. Configure Algolia DocSearch (Free for Open Source)
```javascript
// In docusaurus.config.js
module.exports = {
  themeConfig: {
    algolia: {
      appId: 'YOUR_APP_ID',
      apiKey: 'YOUR_SEARCH_API_KEY',
      indexName: 'YOUR_INDEX_NAME',
    },
  },
};
```

### 2. Alternative: Local Search
```bash
npm install --save @docusaurus/plugin-client-redirects @easyops-cn/docusaurus-search-local
```

## Automation and CI/CD Integration

### 1. Pre-commit Hooks
```bash
# Install pre-commit
pip install pre-commit

# Create .pre-commit-config.yaml
cat > .pre-commit-config.yaml << 'EOF'
repos:
  - repo: local
    hooks:
      - id: docs-validate
        name: Validate documentation
        entry: docs-validate
        language: system
        files: '^docs/.*\.md$'
      - id: markdown-lint
        name: Lint markdown
        entry: markdownlint
        language: system
        files: '^docs/.*\.md$'
EOF

# Install hooks
pre-commit install
```

### 2. GitHub Actions Workflow
```yaml
# .github/workflows/docs.yml
name: Documentation

on:
  push:
    paths: ['docs/**']
  pull_request:
    paths: ['docs/**']

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
      - run: npm ci
      - run: docs-validate docs/ --format json
      - run: docs-build
      - run: docs-serve --port 3000 &
      - run: sleep 10 && curl -f http://localhost:3000
```

## Testing the Complete Setup

### 1. Validation Test
```bash
# Create a test page with intentional issues
echo "# Test Page\n\n[Broken link](./nonexistent.md)" > docs/test.md

# Run validation (should show errors)
docs-validate docs/test.md

# Fix the issues
rm docs/test.md
```

### 2. Build Test
```bash
# Test complete build process
docs-build --validate --verbose

# Verify output
ls -la build/
```

### 3. Navigation Test
```bash
# Start server
docs-serve --port 3000

# Test in browser:
# 1. Navigate to http://localhost:3000
# 2. Check main navigation works
# 3. Test search functionality
# 4. Verify cross-references work
# 5. Check mobile responsiveness
```

## Common Workflows

### Adding New Content
1. Create page using template: `docs-create <template> <title>`
2. Edit content with your preferred editor
3. Validate: `docs-validate <file>`
4. Preview: `docs-serve`
5. Commit changes

### Updating Existing Content
1. Edit the markdown file
2. Update `lastUpdated` in front matter
3. Validate: `docs-validate <file>`
4. Test links: `markdown-link-check <file>`
5. Commit changes

### Publishing New Version
1. Update version in all relevant files
2. Create version snapshot: `docs-version <version>`
3. Build: `docs-build`
4. Deploy: `docs-deploy`
5. Tag in Git: `git tag v<version>`

## Troubleshooting

### Common Issues
- **Build fails**: Check console output for syntax errors
- **Links broken**: Run `docs-validate` to identify broken references
- **Search not working**: Verify Algolia configuration or local search setup
- **Images not loading**: Check file paths and static asset configuration

### Getting Help
- Check `docs-validate` output for specific errors
- Review Docusaurus documentation: https://docusaurus.io/docs
- Examine existing documentation examples in the project

## Next Steps
1. Set up documentation templates for your team
2. Configure automated deployments
3. Train team members on documentation workflows
4. Establish documentation review processes
5. Set up analytics to track documentation usage