# SonarQube Cloud Setup Guide

This guide explains how to configure SonarQube Cloud integration for the Spring Boot Modulith Payment Platform.

## Prerequisites

1. **SonarQube Cloud Account**: Sign up at [SonarCloud.io](https://sonarcloud.io) using your GitHub account
2. **GitHub Repository**: This repository must be accessible to SonarCloud
3. **Admin Access**: You need admin access to this GitHub repository to configure secrets

## Setup Steps

### 1. SonarCloud Project Setup

1. Go to [SonarCloud.io](https://sonarcloud.io) and log in with GitHub
2. Click **"Analyze new project"**
3. Select your GitHub organization and this repository
4. Choose **"With GitHub Actions"** as the analysis method
5. Note down your **Project Key**: `lsendel_sass`
6. Note down your **Organization Key**: `lsendel`

### 2. Generate SonarQube Token

1. In SonarCloud, go to **My Account** → **Security**
2. Generate a new token with a descriptive name (e.g., "sass-github-actions")
3. Copy the token - you'll need it for GitHub secrets
4. **Important**: Store this token securely, you won't be able to see it again

### 3. Configure GitHub Secrets

Go to your GitHub repository → **Settings** → **Secrets and variables** → **Actions** and add:

#### Required Secrets

| Secret Name   | Description                     | How to Get                |
| ------------- | ------------------------------- | ------------------------- |
| `SONAR_TOKEN` | SonarCloud authentication token | Generated in step 2 above |

#### Optional Secrets (for enhanced features)

| Secret Name     | Description                  | How to Get                                  |
| --------------- | ---------------------------- | ------------------------------------------- |
| `CODECOV_TOKEN` | Codecov.io integration token | Sign up at codecov.io and get project token |

### 4. Verify Configuration

The SonarQube analysis will run automatically on:

- **Push to main/develop branches** (when backend code changes)
- **Pull requests to main/develop** (when backend code changes)

## Workflow Details

### What Gets Analyzed

- **Java Code**: All backend source code in `backend/src/main/java`
- **Test Coverage**: JaCoCo coverage reports from backend tests
- **Code Quality**: Checkstyle reports and static analysis
- **Security**: Security hotspots and vulnerability detection
- **Duplications**: Code duplication detection

### Quality Gates

The workflow includes a quality gate check that will:

- ✅ **Pass**: If code meets quality standards
- ❌ **Fail**: If critical issues are found (blocking deployment)

### Reports Generated

1. **Coverage Report**: Test coverage percentage and uncovered lines
2. **Quality Report**: Code smells, bugs, and vulnerabilities
3. **Security Report**: Security hotspots and vulnerabilities
4. **Maintainability**: Technical debt and code complexity

## Viewing Results

### In SonarCloud Dashboard

1. Go to [SonarCloud.io](https://sonarcloud.io)
2. Navigate to your project: `lsendel_sass`
3. View detailed analysis results, trends, and quality gate status

### In GitHub

- **Pull Requests**: SonarCloud will comment with analysis results
- **Actions Tab**: View workflow runs and quality gate status
- **Checks**: Quality gate status appears in PR checks

## Configuration Files

### Backend Gradle Configuration

- **File**: `backend/build.gradle`
- **Plugin**: `org.sonarqube` version 4.4.1.3373
- **Reports**: JaCoCo XML, Checkstyle XML, JUnit XML

### GitHub Actions Workflow

- **File**: `.github/workflows/sonarqube.yml`
- **Triggers**: Push/PR to main/develop with backend changes
- **Services**: PostgreSQL and Redis for testing

### Project Configuration

- **File**: `sonar-project.properties`
- **Purpose**: Project-level SonarCloud configuration
- **Includes**: Quality rules, exclusions, and analysis settings

## Troubleshooting

### Common Issues

#### 1. "Project not found" Error

- Verify `SONAR_TOKEN` is correctly set in GitHub secrets
- Check project key matches: `lsendel_sass`
- Ensure SonarCloud project is properly created

#### 2. Quality Gate Failing

- Review SonarCloud dashboard for specific issues
- Check code coverage requirements (default: 80%)
- Address security vulnerabilities and bugs

#### 3. Analysis Not Running

- Verify workflow file syntax
- Check if changes are in backend directory
- Ensure GitHub Actions are enabled for the repository

#### 4. Permission Errors

- Verify SONAR_TOKEN has proper permissions
- Check if token has expired (tokens expire after 1 year)
- Regenerate token if needed

### Getting Help

1. **SonarCloud Documentation**: [docs.sonarcloud.io](https://docs.sonarcloud.io)
2. **GitHub Actions Logs**: Check workflow run details in Actions tab
3. **SonarCloud Support**: Available through their community forum

## Quality Standards

### Code Coverage

- **Minimum**: 80% line coverage
- **Goal**: 90%+ coverage for critical payment and security modules
- **Exclusions**: Configuration classes, DTOs, test utilities

### Security

- **Zero tolerance**: Critical security vulnerabilities
- **Review required**: Security hotspots in authentication/payment code
- **Regular updates**: Dependency vulnerability scanning

### Maintainability

- **Code Smells**: Address major and critical issues
- **Technical Debt**: Keep under 5% debt ratio
- **Complexity**: Methods should have reasonable cyclomatic complexity

## Integration with Development Workflow

### For Developers

1. **Before Committing**: Run `./gradlew test jacocoTestReport` locally
2. **PR Reviews**: Address SonarCloud findings before merge
3. **Quality Gate**: Ensure quality gate passes before deployment

### For CI/CD Pipeline

- **Automatic Analysis**: Runs on every push/PR
- **Quality Gate**: Blocks merge if quality standards not met
- **Reporting**: Results available in GitHub and SonarCloud

---

**Note**: This setup provides comprehensive code quality analysis for the Spring Boot Modulith Payment Platform, ensuring production-ready code quality and security standards.
